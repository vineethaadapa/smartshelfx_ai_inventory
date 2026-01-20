import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VendorDashboardComponent } from './vendor-dashboard';
describe('VendorDashboard', () => {
  let component: VendorDashboardComponent;
  let fixture: ComponentFixture<VendorDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VendorDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VendorDashboardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
