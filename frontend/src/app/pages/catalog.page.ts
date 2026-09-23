import { Component, computed, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService, errorMessage } from "../services/api.service";
import { Equipment } from "../models/equipment";
import { ProductCardComponent } from "../components/product-card.component";
@Component({
  standalone: true,
  imports: [FormsModule, ProductCardComponent],
  templateUrl: "./catalog.page.html",
})
export class CatalogPage {
  private api = inject(ApiService);
  equipment = signal<Equipment[]>([]);
  loading = signal(true);
  error = signal("");
  category = signal("Todos");
  search = signal("");
  sort = signal("default");
  categories = computed(() => [
    "Todos",
    ...new Set(this.equipment().map((e) => e.category)),
  ]);
  filtered = computed(() => {
    const term = this.search().trim().toLocaleLowerCase("pt-BR");
    const items = this.equipment().filter(
      (e) =>
        (this.category() === "Todos" || e.category === this.category()) &&
        [e.name, e.description, e.category].some((s) =>
          s.toLocaleLowerCase("pt-BR").includes(term),
        ),
    );
    return items.sort((a, b) =>
      this.sort() === "low"
        ? a.price - b.price
        : this.sort() === "high"
          ? b.price - a.price
          : this.sort() === "name"
            ? a.name.localeCompare(b.name)
            : a.id - b.id,
    );
  });
  constructor() {
    void this.load();
  }
  async load() {
    this.loading.set(true);
    this.error.set("");
    try {
      this.equipment.set(await this.api.list());
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }
}
