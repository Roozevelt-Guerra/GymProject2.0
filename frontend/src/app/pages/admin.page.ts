import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { CurrencyPipe } from "@angular/common";
import { Router } from "@angular/router";
import { ApiService, errorMessage } from "../services/api.service";
import { Equipment, EquipmentInput } from "../models/equipment";
@Component({
  imports: [FormsModule, CurrencyPipe],
  templateUrl: "./admin.page.html",
})
export class AdminPage {
  api = inject(ApiService);
  router = inject(Router);
  equipment = signal<Equipment[]>([]);
  message = signal("");
  busy = signal(false);
  loading = signal(false);
  loadFailed = signal(false);
  pendingDelete = signal<Equipment | null>(null);
  editingId: number | undefined;
  form: EquipmentInput = this.blank();
  blank(): EquipmentInput {
    return {
      name: "",
      description: "",
      price: 0,
      imageUrl: "",
      category: "Musculação",
    };
  }
  constructor() {
    void this.load();
  }
  async load() {
    this.loading.set(true);
    this.loadFailed.set(false);
    try {
      this.equipment.set(await this.api.list());
    } catch (e) {
      this.message.set(errorMessage(e));
      this.loadFailed.set(true);
    } finally {
      this.loading.set(false);
    }
  }
  edit(e: Equipment) {
    this.editingId = e.id;
    this.form = {
      name: e.name,
      description: e.description,
      price: e.price,
      imageUrl: e.imageUrl,
      category: e.category,
    };
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  reset() {
    this.editingId = undefined;
    this.form = this.blank();
  }
  async save() {
    this.busy.set(true);
    this.message.set("");
    try {
      await this.api.save(this.form, this.editingId);
      this.reset();
      await this.load();
      this.message.set("Equipamento salvo com sucesso.");
    } catch (e) {
      this.message.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }
  async remove(e: Equipment) {
    this.busy.set(true);
    try {
      await this.api.remove(e.id);
      if (this.editingId === e.id) this.reset();
      this.pendingDelete.set(null);
      await this.load();
      this.message.set("Equipamento excluído.");
    } catch (e) {
      this.message.set(errorMessage(e));
      this.pendingDelete.set(null);
    } finally {
      this.busy.set(false);
    }
  }
  async logout() {
    this.busy.set(true);
    try {
      await this.api.logout();
      await this.router.navigateByUrl("/login");
    } catch (e) {
      this.message.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }
}
