import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatbotRequest {
  message: string;
}

export interface ChatbotResponseData {
  response: string;
  options?: string[];
  route?: string;
}

export interface ChatbotResponseWrapper {
  success: boolean;
  data: ChatbotResponseData;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  // Usar ruta relativa para que pase por Nginx
  private apiUrl = '/api/v1/chatbot';

  constructor(private http: HttpClient) { }

  sendMessage(message: string): Observable<ChatbotResponseWrapper> {
    const request: ChatbotRequest = { message };
    return this.http.post<ChatbotResponseWrapper>(this.apiUrl, request);
  }
}
