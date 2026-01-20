import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-reorder-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './reorder-modal.html',
  styleUrls: ['./reorder-modal.css']
})
export class ReorderModalComponent {
  totalAmount: number = 0;

  constructor(
    public dialogRef: MatDialogRef<ReorderModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.updateTotal();
  }

  updateTotal() {
    const displayQty = Math.max(0, this.data.quantity);
    this.totalAmount = displayQty * this.data.unitPrice;
  }
}