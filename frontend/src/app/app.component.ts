import { Component, inject } from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { CartService } from "./services/cart.service";
@Component({
  selector: "app-root",
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: "./app.component.html",
})
export class AppComponent {
  cart = inject(CartService);
  year = new Date().getFullYear();
}
