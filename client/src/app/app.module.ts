import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { AppComponent } from './app.component';
import { StormpathConfiguration, StormpathModule } from 'angular-stormpath';
import { BeerListComponent } from './beer-list/beer-list.component';
import { MaterialModule } from '@angular/material';
import { AppShellModule } from '@angular/app-shell';
import { environment } from '../environments/environment';

export function stormpathConfig(): StormpathConfiguration {
  let spConfig: StormpathConfiguration = new StormpathConfiguration();
  if (environment.production) {
    spConfig.endpointPrefix = 'https://pwa-server.cfapps.io';
  } else {
    spConfig.endpointPrefix = 'http://localhost:8080';
  }  return spConfig;
}

@NgModule({
  declarations: [
    AppComponent,
    BeerListComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    MaterialModule,
    AppShellModule.runtime(),
    StormpathModule
  ],
  providers: [{
    provide: StormpathConfiguration, useFactory: stormpathConfig
  }],
  bootstrap: [AppComponent]
})
export class AppModule { }
