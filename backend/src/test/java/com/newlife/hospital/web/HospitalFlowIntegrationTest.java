package com.newlife.hospital.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HospitalFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private Cookie login(String username, String password, String portal) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","portal":"%s"}""".formatted(username, password, portal)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("NLH_SESSION", true))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn();
        return result.getResponse().getCookie("NLH_SESSION");
    }

    @Test
    void publicContentIsAvailableAnonymously() throws Exception {
        mvc.perform(get("/api/public/cms")).andExpect(status().isOk()).andExpect(jsonPath("$", hasKey("mission")));
        mvc.perform(get("/api/public/doctors")).andExpect(status().isOk());
        mvc.perform(get("/api/validations").param("forms", "patient"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].formName").value("patient"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isNoContent());
    }

    @Test
    void portalsAreRoleRestricted() throws Exception {
        mvc.perform(get("/api/staff/patients")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff1\",\"password\":\"Staff@NLH2026\",\"portal\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        Cookie staff = login("staff1", "Staff@NLH2026", "STAFF");
        mvc.perform(get("/api/admin/dashboard").cookie(staff)).andExpect(status().isForbidden());

        Cookie admin = login("admin", "Admin@NLH2026", "ADMIN");
        mvc.perform(get("/api/admin/dashboard").cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/staff/patients").cookie(admin)).andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff1\",\"password\":\"Staff@NLH2026\",\"portal\":\"STAFF\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsRejectedGenerically() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever\",\"portal\":\"STAFF\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void registerPatientEncryptsIdPreventsDuplicatesAndBills() throws Exception {
        Cookie staff = login("staff1", "Staff@NLH2026", "STAFF");
        String body = """
                {"name":"Ravi Kumar","idType":"AADHAAR","idNumber":"2345 6789 0123 4567","dob":"1990-05-17",
                 "gender":"MALE","contactNumber":"9876543210","address":"Sanwer Road, Indore"}""";

        // DB-driven validation rejects bad input with the configured messages.
        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("9876543210", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contactNumber").value(
                        "Contact number must be a valid 10-digit Indian mobile number."));
        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("2345 6789 0123 4567", "2345 6789 0123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idNumber").value("Aadhaar number must be exactly 16 digits."));

        String licence = """
                {"name":"Meena Joshi","idType":"DRIVING_LICENSE","idNumber":"%s","dob":"1988-02-01",
                 "gender":"FEMALE","contactNumber":"9876500000"}""";
        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(licence.formatted("XX09 2019 0012345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idNumber").exists());
        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(licence.formatted("MP09 1899 0012345")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(licence.formatted("mp-09 2019 0012345")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedIdNumber").value("XXXX-2345"));

        var created = mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedIdNumber").value("XXXX-4567"))
                .andReturn();
        long patientId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String stored = jdbc.queryForObject("select id_number from patients where id = ?", String.class, patientId);
        assertThat(stored).doesNotContain("2345678901234567");

        mvc.perform(post("/api/staff/patients").with(csrf()).cookie(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/staff/patients/check-id").with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idType\":\"AADHAAR\",\"idNumber\":\"2345678901234567\"}"))
                .andExpect(jsonPath("$.exists").value(true));
        mvc.perform(get("/api/staff/patients/suggest").param("q", "ravi").cookie(staff))
                .andExpect(jsonPath("$[0].id").value(patientId));

        // Only contact number and address may be edited.
        mvc.perform(patch("/api/staff/patients/" + patientId).with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactNumber\":\"9123456780\",\"address\":\"Ujjain\",\"name\":\"Hacked\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/staff/patients/" + patientId).with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactNumber\":\"9123456780\",\"address\":\"Ujjain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ravi Kumar"))
                .andExpect(jsonPath("$.contactNumber").value("9123456780"));

        // Billing: prices come from the charges master, never from the client.
        JsonNode services = json.readTree(mvc.perform(get("/api/public/services")).andReturn()
                .getResponse().getContentAsString());
        long serviceId = services.get(0).get("id").asLong();
        double cost = services.get(0).get("cost").asDouble();
        mvc.perform(post("/api/staff/invoices").with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"items":[{"serviceId":%d,"quantity":1}],"paymentStatus":"FULL",
                                 "visitAt":"2099-01-01T04:30:00.000Z"}""".formatted(patientId, serviceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.visitAt").value("Visit time cannot be in the future."));
        var invoice = mvc.perform(post("/api/staff/invoices").with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"items":[{"serviceId":%d,"quantity":2}],"discount":10,
                                 "paymentStatus":"PARTIAL","paidAmount":50}""".formatted(patientId, serviceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value(containsString("NLH-INV-")))
                .andExpect(jsonPath("$.staffName").value("Demo Staff"))
                .andExpect(jsonPath("$.visitAt").exists())
                .andExpect(jsonPath("$.totalAmount").value(cost * 2))
                .andExpect(jsonPath("$.balanceAmount").value(cost * 2 - 10 - 50))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIAL"))
                .andReturn();
        long invoiceId = json.readTree(invoice.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/staff/invoices/" + invoiceId + "/payments").with(csrf()).cookie(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%s}".formatted(cost * 2 - 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FULL"))
                .andExpect(jsonPath("$.balanceAmount").value(0));

        Cookie admin = login("admin", "Admin@NLH2026", "ADMIN");
        mvc.perform(get("/api/admin/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.invoiceCount").value(1))
                .andExpect(jsonPath("$.daily.length()").value(30))
                .andExpect(jsonPath("$.monthly.length()").value(12));
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        Cookie staff = login("staff1", "Staff@NLH2026", "STAFF");
        mvc.perform(get("/api/auth/me").cookie(staff)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(staff)).andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/patients").cookie(staff)).andExpect(status().isUnauthorized());
    }
}
