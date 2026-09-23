import { inject, Injectable } from "@angular/core";
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
} from "@angular/common/http";
import { firstValueFrom } from "rxjs";
import { Equipment, EquipmentInput } from "../models/equipment";
@Injectable({ providedIn: "root" })
export class ApiService {
  private http = inject(HttpClient);
  list() {
    return firstValueFrom(this.http.get<Equipment[]>("/api/equipment"));
  }
  get(id: number) {
    return firstValueFrom(this.http.get<Equipment>("/api/equipment/" + id));
  }
  config() {
    return firstValueFrom(
      this.http.get<{ whatsappNumber: string }>("/api/config"),
    );
  }
  me() {
    return firstValueFrom(this.http.get<{ username: string }>("/api/auth/me"));
  }
  async write<T>(
    method: string,
    url: string,
    body: unknown = null,
    form = false,
  ): Promise<T> {
    const csrf = await firstValueFrom(
      this.http.get<{ token: string; headerName: string }>("/api/auth/csrf"),
    );
    let headers = new HttpHeaders().set(csrf.headerName, csrf.token);
    if (form)
      headers = headers.set(
        "Content-Type",
        "application/x-www-form-urlencoded",
      );
    return firstValueFrom(this.http.request<T>(method, url, { body, headers }));
  }
  login(username: string, password: string) {
    return this.write(
      "POST",
      "/api/auth/login",
      new URLSearchParams({ username, password }).toString(),
      true,
    );
  }
  logout() {
    return this.write("POST", "/api/auth/logout");
  }
  save(data: EquipmentInput, id?: number) {
    return this.write<Equipment>(
      id ? "PUT" : "POST",
      "/api/equipment" + (id ? "/" + id : ""),
      data,
    );
  }
  remove(id: number) {
    return this.write<void>("DELETE", "/api/equipment/" + id);
  }
}
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401)
      return "Sua sessão expirou ou as credenciais são inválidas. Faça login novamente.";
    if (error.status === 403)
      return "Não foi possível autorizar esta ação. Atualize a página e tente novamente.";
    if (error.status === 404) return "Equipamento não encontrado.";
    if (error.status === 400)
      return error.error?.message || "Confira os dados informados.";
  }
  return "Não foi possível conectar ao servidor. Tente novamente.";
}
