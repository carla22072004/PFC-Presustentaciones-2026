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
  private apiUrl = '/api/v1/chatbot/ask';

  constructor(private http: HttpClient) { }

  sendMessage(message: string): Observable<ChatbotResponseData> {
    const request: ChatbotRequest = { message };
    // Debido a auth.interceptor.ts que desempaqueta ResponseWrapper,
    // el HTTP Client retorna directamente ChatbotResponseData.
    return this.http.post<ChatbotResponseData>(this.apiUrl, request);
  }
}
