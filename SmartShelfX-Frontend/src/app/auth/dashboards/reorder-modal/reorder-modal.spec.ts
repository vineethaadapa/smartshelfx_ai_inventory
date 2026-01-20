import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReorderModal } from './reorder-modal';

describe('ReorderModal', () => {
  let component: ReorderModal;
  let fixture: ComponentFixture<ReorderModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReorderModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReorderModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
