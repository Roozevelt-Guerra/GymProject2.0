export interface Equipment {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  category: string;
}
export type EquipmentInput = Omit<Equipment, "id">;
export interface CartItem {
  equipment: Equipment;
  quantity: number;
}
