import { Component, OnInit } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth/AuthService';

export const passwordMatchValidator = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  if (password && confirmPassword && password.value !== confirmPassword.value) {
    return { mismatch: true }; 
  }
  return null;
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule], 
  templateUrl: './register.html',
  styleUrl: './register.css' 
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;

  constructor(
      private fb: FormBuilder, 
      private router: Router,
      private authService: AuthService 
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      role: ['', Validators.required], // ⬅️ NEW: Added role control
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { 
      validator: passwordMatchValidator 
    });
  }

  onRegisterSubmit(): void {
    if (this.registerForm.valid) {
      const registerData = {
          name: this.registerForm.value.name,
          email: this.registerForm.value.email,
          password: this.registerForm.value.password,
          role: this.registerForm.value.role // ⬅️ NEW: Sending role to backend
      };

      this.authService.register(registerData).subscribe({
        next: (response) => {
          console.log('Registration Successful!', response);
          alert('Registration successful! Redirecting to login.');
          this.router.navigate(['/login']);
        },
        error: (error) => {
          console.error('Registration Failed:', error);
          let errorMessage = error.error?.message || 'Registration failed.';
          alert(`Registration failed: ${errorMessage}`); 
        }
      });

    } else {
      alert('Please fill out the form correctly.');
    }
  }
}