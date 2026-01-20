import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; 
import { authInterceptor } from './services/auth/auth.interceptor'; 

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    importProvidersFrom(ReactiveFormsModule), 

    provideHttpClient(
      withInterceptors([authInterceptor]) 
    )
  ]
};