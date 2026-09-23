import { Component, input, inject } from "@angular/core";
import { CurrencyPipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { Equipment } from "../models/equipment";
import { CartService } from "../services/cart.service";
@Component({
  selector: "app-product-card",
  imports: [CurrencyPipe, RouterLink],
  templateUrl: "./product-card.component.html",
})
export class ProductCardComponent {
  equipment = input.required<Equipment>();
  cart = inject(CartService);
  imageError(e: Event) {
    const img = e.target as HTMLImageElement;
    img.onerror = null;
    img.src = "/assets/maqpro.png";
  }
}
