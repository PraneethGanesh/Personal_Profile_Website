import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-contact',
  imports: [FormsModule],
  templateUrl: './contact.component.html'
})
export class ContactComponent {
  private readonly http = inject(HttpClient);
  readonly contactEnabled = !!environment.apiBaseUrl.trim();
  readonly pending = signal(false);
  readonly status = signal('');
  readonly failed = signal(false);

  clearMessage(form: NgForm): void {
    if (this.pending()) { return; }
    form.resetForm();
    this.status.set('');
    this.failed.set(false);
  }

  submitMessage(form: NgForm): void {
    if (!this.contactEnabled || this.pending()) { return; }
    this.status.set('');
    this.failed.set(false);
    if (form.invalid) { return; }
    const { fullName, email, message } = form.value as { fullName: string; email: string; message: string };
    if (!fullName.trim() || !message.trim()) {
      form.controls['fullName']?.setErrors(fullName.trim() ? null : { required: true });
      form.controls['message']?.setErrors(message.trim() ? null : { required: true });
      return;
    }
    this.pending.set(true);
    this.http.post(`${environment.apiBaseUrl.replace(/\/$/, '')}/api/contact`, {
      name: fullName.trim(), email: email.trim(), message: message.trim()
    }, { observe: 'response' }).pipe(timeout(20000), finalize(() => this.pending.set(false))).subscribe({
      next: response => {
        if (response.status === 202) {
          this.status.set('Thanks. Your message was accepted');
          form.resetForm();
        } else {
          this.failed.set(true);
          this.status.set('The server did not confirm acceptance. Your message is still here.');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.failed.set(true);
        this.status.set(error.status === 429
          ? 'Too many requests. Please wait a minute and try again. Your message is still here.'
          : 'Your message could not be confirmed. Please try again. Your message is still here.');
      }
    });
  }
}
