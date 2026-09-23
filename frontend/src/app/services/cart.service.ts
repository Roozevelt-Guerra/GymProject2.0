import { computed, Injectable, signal } from "@angular/core";
import { CartItem, Equipment } from "../models/equipment";
@Injectable({ providedIn: "root" })
export class CartService {
  readonly items = signal<CartItem[]>(this.load());
  readonly count = computed(() =>
    this.items().reduce((n, i) => n + i.quantity, 0),
  );
  readonly total = computed(
    () =>
      this.items().reduce(
        (n, i) => n + Math.round(i.equipment.price * 100) * i.quantity,
        0,
      ) / 100,
  );
  notice = signal("");
  private load(): CartItem[] {
    try {
      const data: unknown = JSON.parse(
        localStorage.getItem("maqpro-cart") || "[]",
      );
      return Array.isArray(data)
        ? data
            .filter(
              (i) =>
                i &&
                i.equipment &&
                Number.isSafeInteger(i.equipment.id) &&
                i.equipment.id > 0 &&
                typeof i.equipment.name === "string" &&
                typeof i.equipment.imageUrl === "string" &&
                Number.isFinite(i.equipment.price) &&
                i.equipment.price > 0 &&
                Number.isInteger(i.quantity) &&
                i.quantity > 0 &&
                i.quantity <= 99,
            )
            .slice(0, 100)
        : [];
    } catch {
      return [];
    }
  }
  private persist() {
    try {
      localStorage.setItem("maqpro-cart", JSON.stringify(this.items()));
    } catch {
      this.notice.set(
        "O navegador não permitiu salvar o carrinho. Ele será mantido apenas nesta sessão.",
      );
    }
  }
  add(equipment: Equipment, quantity = 1) {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return;
    this.items.update((items) => {
      const found = items.find((i) => i.equipment.id === equipment.id);
      return found
        ? items.map((i) =>
            i === found
              ? { equipment, quantity: Math.min(99, i.quantity + quantity) }
              : i,
          )
        : [...items, { equipment, quantity }];
    });
    this.persist();
    this.notice.set(equipment.name + " adicionado ao carrinho.");
  }
  quantity(id: number, quantity: number) {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return;
    this.items.update((items) =>
      items.map((i) => (i.equipment.id === id ? { ...i, quantity } : i)),
    );
    this.persist();
  }
  remove(id: number) {
    this.items.update((items) => items.filter((i) => i.equipment.id !== id));
    this.persist();
  }
  reconcile(equipment: Equipment[]): boolean {
    let changed = false;
    this.items.update((items) =>
      items.flatMap((i) => {
        const current = equipment.find((e) => e.id === i.equipment.id);
        if (!current) {
          changed = true;
          return [];
        }
        if (
          current.price !== i.equipment.price ||
          current.name !== i.equipment.name
        )
          changed = true;
        return [{ equipment: current, quantity: i.quantity }];
      }),
    );
    this.persist();
    return changed;
  }
}
