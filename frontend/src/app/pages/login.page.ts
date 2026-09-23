import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ApiService, errorMessage } from "../services/api.service";
@Component({ imports: [FormsModule], templateUrl: "./login.page.html" })
export class LoginPage {
  api = inject(ApiService);
  router = inject(Router);
  username = "";
  password = "";
  error = signal("");
  busy = signal(false);
  async login() {
    this.busy.set(true);
    this.error.set("");
    try {
      await this.api.login(this.username, this.password);
      await this.router.navigateByUrl("/admin");
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }
}
