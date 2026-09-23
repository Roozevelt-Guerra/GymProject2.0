import { inject } from "@angular/core";
import { Routes, Router } from "@angular/router";
import { ApiService } from "./services/api.service";
export const routes: Routes = [
  {
    path: "",
    loadComponent: () =>
      import("./pages/catalog.page").then((m) => m.CatalogPage),
  },
  {
    path: "equipamentos/:id",
    loadComponent: () =>
      import("./pages/detail.page").then((m) => m.DetailPage),
  },
  {
    path: "carrinho",
    loadComponent: () => import("./pages/cart.page").then((m) => m.CartPage),
  },
  {
    path: "login",
    loadComponent: () => import("./pages/login.page").then((m) => m.LoginPage),
  },
  {
    path: "admin",
    canActivate: [
      () => {
        const router = inject(Router);
        return inject(ApiService)
          .me()
          .then(() => true)
          .catch(() => router.createUrlTree(["/login"]));
      },
    ],
    loadComponent: () => import("./pages/admin.page").then((m) => m.AdminPage),
  },
  { path: "**", redirectTo: "" },
];
