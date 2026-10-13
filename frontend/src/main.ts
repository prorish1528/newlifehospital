import { registerLocaleData } from '@angular/common';
import localeEnIn from '@angular/common/locales/en-IN';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID, mergeApplicationConfig } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Indian number grouping (₹1,23,456.00) and INR as the default currency.
registerLocaleData(localeEnIn);

const config = mergeApplicationConfig(appConfig, {
  providers: [
    { provide: LOCALE_ID, useValue: 'en-IN' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'INR' },
  ],
});

bootstrapApplication(App, config).catch((err) => console.error(err));
