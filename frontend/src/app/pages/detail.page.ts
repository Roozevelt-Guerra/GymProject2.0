import { Component, inject, signal } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { CurrencyPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ApiService, errorMessage } from "../services/api.service";
import { CartService } from "../services/cart.service";
import { Equipment } from "../models/equipment";
@Component({
  imports: [RouterLink, CurrencyPipe, FormsModule],
  templateUrl: "./detail.page.html",
})
export class DetailPage {
  api = inject(ApiService);
  cart = inject(CartService);
  route = inject(ActivatedRoute);
  equipment = signal<Equipment | null>(null);
  error = signal("");
  quantity = 1;
  constructor() {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    this.api
      .get(id)
      .then((e) => this.equipment.set(e))
      .catch((e) => this.error.set(errorMessage(e)));
  }
  valid() {
    return (
      Number.isInteger(this.quantity) &&
      this.quantity >= 1 &&
      this.quantity <= 99
    );
  }
}
