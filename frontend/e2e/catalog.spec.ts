import { test, expect } from "@playwright/test";

test("admin CRUD, cart persistence, quote, responsive layout and logout", async ({
  page,
}) => {
  const name = "Equipamento teste " + Date.now();
  await page.goto("/admin");
  await expect(page).toHaveURL(/login/);
  await page
    .getByLabel("Usuário")
    .fill(process.env["E2E_ADMIN_USERNAME"] || "admin");
  await page
    .getByLabel("Senha", { exact: true })
    .fill(process.env["E2E_ADMIN_PASSWORD"] || "test-password-123");
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/admin/);
  await page.getByLabel("Nome", { exact: true }).fill(name);
  await page.getByLabel("Categoria", { exact: true }).fill("Musculação");
  await page
    .getByLabel("Descrição", { exact: true })
    .fill("Equipamento temporário para validar o fluxo completo.");
  await page.getByLabel("Preço (R$)").fill("1250.50");
  await page
    .getByLabel("URL da imagem (HTTPS)")
    .fill("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600");
  await page.getByRole("button", { name: "Cadastrar equipamento" }).click();
  await expect(page.getByRole("status")).toContainText("salvo com sucesso");
  const item = page.locator(".admin-item").filter({ hasText: name });
  await item.getByRole("button", { name: "Editar", exact: true }).click();
  await page.getByLabel("Preço (R$)").fill("1500.25");
  await page.getByRole("button", { name: "Salvar alterações" }).click();
  await expect(item).toContainText("1.500,25");
  await page.getByRole("link", { name: "Catálogo", exact: true }).click();
  await page.getByLabel("Buscar equipamento").fill(name);
  await expect(page.locator("app-product-card")).toHaveCount(1);
  await page.getByRole("link", { name: "Conhecer equipamento" }).click();
  await page.getByLabel("Quantidade").fill("2");
  await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
  await page.getByRole("link", { name: "Ver meu orçamento" }).click();
  await expect(page.locator(".summary-total")).toContainText("3.000,50");
  await page.reload();
  await expect(page.locator(".summary-total")).toContainText("3.000,50");
  await page.getByLabel("Qtd.").fill("3");
  await expect(page.locator(".summary-total")).toContainText("4.500,75");
  let quote = "";
  await page.route("https://wa.me/**", async (route) => {
    quote = route.request().url();
    await route.fulfill({
      contentType: "text/html",
      body: "WhatsApp interceptado no teste",
    });
  });
  await page.getByRole("button", { name: "Solicitar orçamento" }).click();
  await expect.poll(() => quote).toContain("https://wa.me/");
  const text = new URL(quote).searchParams.get("text") || "";
  expect(text).toContain(name);
  expect(text).toContain("3 un.");
  expect(text).toContain("4.500,75");
  await page.goto("/");
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(
    page.getByRole("heading", { name: /Sua academia/ }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/catalog-mobile.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({
    path: "test-results/catalog-desktop.png",
    fullPage: true,
  });
  await page.goto("/admin");
  await page
    .locator(".admin-item")
    .filter({ hasText: name })
    .getByRole("button", { name: "Excluir", exact: true })
    .click();
  await page.getByRole("button", { name: "Confirmar exclusão" }).click();
  await expect(page.getByRole("status")).toContainText("excluído");
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await expect(page).toHaveURL(/login/);
  await page.goto("/carrinho");
  await page.getByRole("button", { name: "Solicitar orçamento" }).click();
  await expect(page.getByRole("status")).toContainText(
    "removemos itens indisponíveis",
  );
  await expect(
    page.getByRole("heading", {
      name: "Seu próximo projeto começa no catálogo.",
    }),
  ).toBeVisible();
});
