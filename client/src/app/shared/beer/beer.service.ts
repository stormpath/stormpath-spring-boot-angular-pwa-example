import { Injectable } from '@angular/core';
import { Http, RequestOptions, Response } from '@angular/http';
import { StormpathConfiguration } from 'angular-stormpath';
import { Observable } from 'rxjs';

@Injectable()
export class BeerService {

  constructor(public http: Http, public config: StormpathConfiguration) {}

  getAll(): Observable<any> {
    let options = new RequestOptions({ withCredentials: true });

    return this.http.get(this.config.endpointPrefix + '/good-beers', options)
      .map((response: Response) => response.json());
  }
}
