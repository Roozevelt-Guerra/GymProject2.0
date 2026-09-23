import { Component, inject, signal } from "@angular/core";
import { CurrencyPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { CartService } from "../services/cart.service";
import { ApiService, errorMessage } from "../services/api.service";
@Component({
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: "./cart.page.html",
})
export class CartPage {
  cart = inject(CartService);
  api = inject(ApiService);
  busy = signal(false);
  message = signal("");
  whatsappUrl = signal("");
  change(id: number, q: number) {
    this.cart.quantity(id, q);
    this.whatsappUrl.set("");
    if (!Number.isInteger(q) || q < 1 || q > 99)
      this.message.set("Informe uma quantidade inteira entre 1 e 99.");
    else this.message.set("");
  }
  async quote() {
    this.busy.set(true);
    this.message.set("");
    this.whatsappUrl.set("");
    try {
      const [items, config] = await Promise.all([
        this.api.list(),
        this.api.config(),
      ]);
      const changed = this.cart.reconcile(items);
      if (changed) {
        this.message.set(
          "O catálogo mudou. Atualizamos preços e removemos itens indisponíveis. Revise o carrinho e solicite novamente.",
        );
        return;
      }
      if (!this.cart.items().length) return;
      if (!config.whatsappNumber) {
        this.message.set(
          "O WhatsApp da empresa ainda não foi configurado. Entre em contato com o administrador.",
        );
        return;
      }
      const brl = (n: number) =>
        n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
      const text = [
        "Olá, MAQPRO! Gostaria de solicitar um orçamento:",
        ...this.cart
          .items()
          .map(
            (i) =>
              `• ${i.equipment.name} | ${i.quantity} un. × ${brl(i.equipment.price)} = ${brl((Math.round(i.equipment.price * 100) * i.quantity) / 100)}`,
          ),
        `Total estimado: ${brl(this.cart.total())}`,
        "Por favor, informe frete, prazo e condições de pagamento.",
      ].join("\n");
      const url =
        "https://wa.me/" +
        config.whatsappNumber +
        "?text=" +
        encodeURIComponent(text);
      this.whatsappUrl.set(url);
      window.location.assign(url);
    } catch (e) {
      this.message.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }
}
