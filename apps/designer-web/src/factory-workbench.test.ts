import { describe, expect, it } from "vitest";
import { FACTORY_ROLES, type FactoryActor } from "@doormes/contracts/factory-workflow";
import { factoryPagesFor } from "./factory-workbench.js";
describe("factory role workbench routing", () => {
  it("opens each role at its own responsibility without exposing account management", () => {
    for (const role of FACTORY_ROLES) {
      const actor: FactoryActor = { id: role, name: role, organizationId: "factory-prototype", roles: [role] };
      const pages = factoryPagesFor(actor);
      expect(pages[0]).toBe(role === "admin" ? "users" : role === "sales" ? "orders" : role === "designer" ? "tasks" : role === "operator" ? "packages" : "reviews");
      expect(pages.includes("users")).toBe(role === "admin");
      expect(new Set(pages).size).toBe(pages.length);
    }
  });
});
