import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatbotService } from '../../services/chatbot.service';

interface ChatMessage {
  text: string;
  isBot: boolean;
  options?: string[];
  route?: string;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements OnInit {
  isOpen = false;
  messages: ChatMessage[] = [];
  userInput = '';
  isTyping = false;

  @ViewChild('chatBody') private chatBody!: ElementRef;

  constructor(private chatbotService: ChatbotService) {}

  ngOnInit(): void {
    this.messages.push({
      text: 'Hola 👋 Soy el asistente virtual de Pre-Sustentaciones UTEQ. ¿En qué puedo ayudarte?',
      isBot: true,
      options: ['📋 Solicitudes', '📄 Anteproyecto', '🔔 Notificaciones', '👤 Mi Perfil', '📅 Sustentación', '🔐 Contraseña', '❓ Ayuda']
    });
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.scrollToBottom();
    }
  }

  sendMessage(text: string = this.userInput): void {
    if (!text.trim()) return;

    // Remove emoji from options for sending
    const cleanText = text.replace(/^[\u2700-\u27BF]|[\uE000-\uF8FF]|\uD83C[\uDC00-\uDFFF]|\uD83D[\uDC00-\uDFFF]|[\u2011-\u26FF]|\uD83E[\uDD10-\uDDFF]\s*/g, '').trim();

    this.messages.push({ text, isBot: false });
    this.userInput = '';
    this.isTyping = true;
    this.scrollToBottom();

    this.chatbotService.sendMessage(cleanText).subscribe({
      next: (res) => {
        this.isTyping = false;
        if (res.success && res.data) {
          this.messages.push({
            text: res.data.response,
            isBot: true,
            options: res.data.options,
            route: res.data.route
          });
        }
        this.scrollToBottom();
      },
      error: () => {
        this.isTyping = false;
        this.messages.push({
          text: 'Hubo un error al conectar con el servidor. Inténtalo más tarde.',
          isBot: true
        });
        this.scrollToBottom();
      }
    });
  }

  selectOption(option: string): void {
    this.sendMessage(option);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    }, 100);
  }
}
