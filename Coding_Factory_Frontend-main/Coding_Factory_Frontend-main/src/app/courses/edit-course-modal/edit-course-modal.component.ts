import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { CourseService } from '../../services/course.service';
import { Course } from '../../models/courses';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-edit-course-modal',
  templateUrl: './edit-course-modal.component.html',
  styleUrls: ['./edit-course-modal.component.scss', '../course.component.scss', '../addtional css/advanced-cards.component.scss', '../addtional css/crud-modal.component.scss']
})
export class EditCourseModalComponent implements OnInit {
  @Input() showModal: boolean = false;
  @Input() courseId!: number;
  @Output() showModalChange = new EventEmitter<boolean>();
  @Output() courseUpdated = new EventEmitter<Course>();

  categoryEnum = ["WEB_DEVELOPMENT", "DATA_SCIENCE", "SECURITY", "AI", "CLOUD"];
  course: Course = new Course();

  constructor(private courseService: CourseService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    if (this.courseId) {
      this.loadCourseData();
    }
  }

  ngOnChanges(): void {
    if (this.courseId) {
      this.loadCourseData();
    }
  }

  loadCourseData(): void {
    this.courseService.getCourseById(this.courseId).subscribe(
      (data) => {
        this.course = data;
        this.cdr.detectChanges(); // Manually trigger change detection
      },
      (error) => {
        console.error('Error fetching course:', error);
      }
    );
  }

  closeModal(): void {
    this.showModal = false;
    this.showModalChange.emit(this.showModal);
  }

  updateCourse(): void {
    this.courseService.updateCourse(this.courseId, this.course).subscribe(
      (updatedCourse) => {
        this.courseUpdated.emit(updatedCourse);
        this.closeModal();
        Swal.fire({
          title: 'Success!',
          text: 'Course updated successfully!',
          icon: 'success',
          confirmButtonText: 'OK'
        });
      },
      (error) => {
        console.error('Error updating course:', error);
      }
    );
  }
}
