import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient } from "@angular/common/http";
import { provideRouter, withInMemoryScrolling } from "@angular/router";
import { registerLocaleData } from "@angular/common";
import pt from "@angular/common/locales/pt";
import { LOCALE_ID } from "@angular/core";
import { AppComponent } from "./app/app.component";
import { routes } from "./app/app.routes";
registerLocaleData(pt);
bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    provideRouter(
      routes,
      withInMemoryScrolling({
        anchorScrolling: "enabled",
        scrollPositionRestoration: "enabled",
      }),
    ),
    { provide: LOCALE_ID, useValue: "pt-BR" },
  ],
}).catch(console.error);
