
import { Component, OnInit } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth/AuthService'; 
@Component({
  selector: 'app-login',
  standalone: true, // Must be standalone
  imports: [CommonModule, RouterLink, ReactiveFormsModule], // Import modules/directives for use in template
  templateUrl: './login.html', // Point to the HTML file
  // If you are using 'login.css', ensure styleUrl is present:
  styleUrl: './login.css' 
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;

  // 🛑 INJECT THE AUTH SERVICE
  constructor(
      private fb: FormBuilder, 
      private router: Router,
      private authService: AuthService // ⬅️ INJECTED
  ) {}

  ngOnInit(): void {
    // Initialize the form group with controls and validation
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onLoginSubmit(): void {
    if (this.loginForm.valid) {
      const loginData = this.loginForm.value;

      this.authService.login(loginData).subscribe({
        next: (response) => {
          console.log('Login Successful!');
          
          // 🛡️ Get the role from the service (which decodes the JWT)
          const role = this.authService.getUserRole(); 

          // 🚀 Route based on role
          if (role === 'ADMIN') {
            this.router.navigate(['/admin-dashboard']);
          } else if (role === 'MANAGER') {
            this.router.navigate(['/manager-dashboard']);
          } else if (role === 'VENDOR') {
            this.router.navigate(['/vendor-dashboard']);
          } else {
            // Fallback for unexpected roles
            this.router.navigate(['/dashboard']); 
          }
        },
        error: (error) => {
          console.error('Login Failed:', error);
          alert(`Login failed: ${error.statusText || 'Invalid credentials'}.`); 
        }
      });
    } else {
      alert('Please fill out the form correctly.');
    }
  }
}