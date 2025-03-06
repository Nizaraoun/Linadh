import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CourseService } from '../services/course.service';
import { CourseResourceService } from '../services/course-resource.service';
import { Course } from '../models/courses';
import { User } from '../models/User';
import { CategoryEnum } from '../models/CategoryEnum';
import Swal from 'sweetalert2';
import { CourseResource } from 'app/models/CourseResource';

@Component({
  selector: 'app-course',
  templateUrl: './course.component.html',
  styleUrls: ['./course.component.scss', './addtional css/advanced-cards.component.scss', './addtional css/crud-modal.component.scss','./addtional css/badges.component.scss']
})
export class CourseComponent implements OnInit {
  categoryEnum = CategoryEnum;
  categoryColors: { [key: string]: string } = {};
  courses: Course[] = [];
  filteredCourses: Course[] = [];
  selectedCourse: Course | null = null;
  trainers: User[] = [];
  showModal = false;
  showAddResourceModal = false;
  showResourcesModal = false;
  showEditModal = false;
  searchQuery = '';
  selectedCategory = '';

  constructor(private courseService: CourseService, private courseResourceService: CourseResourceService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.getAllCourses();
    this.assignRandomColorsToCategories(); // Assign random colors to categories
  }

  // Function to generate random colors
  generateRandomColor(): string {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

  // Function to assign random colors to each category in CategoryEnum
  assignRandomColorsToCategories(): void {
    Object.keys(this.categoryEnum).forEach(category => {
      this.categoryColors[category] = this.generateRandomColor();  // Assign a color to each category
    });
  }

  getAllCourses(): void {
    this.courseService.getAllCourses().subscribe(
      (data) => {
        this.courses = data.map(course => ({
          ...course,
          image: this.getFile(course.image)  // Ensure image URL is correct
        }));
        this.filterCourses();
        this.cdr.detectChanges(); // Ensure view updates
      },
      (error) => {
        console.error('Error fetching courses:', error);
      }
    );
}


getFile(fileName: string): string {
  // If `fileName` already contains the full URL, return it directly.
  if (fileName.startsWith('https://')) {
    return fileName;  // No need to prepend the base URL again
  }
  
  // If it's just a file name, prepend the base URL.
  return `https://wbptqnvcpiorvwjotqwx.supabase.co/storage/v1/object/public/course-images/${fileName}`;
}

  
  
  
  
  filterCourses(): void {
    console.log('Filtering with searchQuery:', this.searchQuery);
    console.log('Filtering with selectedCategory:', this.selectedCategory);
  
    this.filteredCourses = this.courses.filter(course =>
      (this.searchQuery === '' || course.title.toLowerCase().includes(this.searchQuery.toLowerCase())) &&
      (this.selectedCategory === '' || course.categoryCourse.toLowerCase() === this.selectedCategory.toLowerCase())
    );
    
    console.log('Filtered Courses:', this.filteredCourses); // Log filtered courses
    
    this.cdr.detectChanges(); // Ensure UI updates
  }
  
  

  openAddResourceModal(course: Course): void {
    this.selectedCourse = course;
    this.showAddResourceModal = true;
    this.showResourcesModal = false;
    this.cdr.detectChanges();
  }

  openAddCourseModal(): void {
    this.showModal = true;
    this.cdr.detectChanges();
  }

  onCourseAdded(course: Course): void {
    this.courses.push(course);
    this.filterCourses();
    this.cdr.detectChanges();
  }

  onResourceAdded(resource: CourseResource): void {
    if (this.selectedCourse && this.selectedCourse.resources) {
      this.selectedCourse.resources.push(resource);
      this.cdr.detectChanges();
    }
  }

  openResourcesModal(course: Course): void {
    this.selectedCourse = course;
    this.showResourcesModal = true;
    this.showAddResourceModal = false;
    this.courseResourceService.getResourcesForCourse(course.id).subscribe(
      (resources) => {
        this.selectedCourse!.resources = resources;
        this.cdr.detectChanges();
      },
      (error) => {
        console.error('Error fetching resources:', error);
      }
    );
  }

  deleteCourse(courseId: number): void {
    Swal.fire({
      title: 'Are you sure?',
      text: 'You will not be able to recover this course!',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, delete it!',
      cancelButtonText: 'No, keep it'
    }).then((result) => {
      if (result.isConfirmed) {
        this.courseService.deleteCourse(courseId).subscribe(() => {
          this.getAllCourses();
          Swal.fire('Deleted!', 'The course has been deleted.', 'success');
        }, (error) => {
          console.error('Error deleting course:', error);
          Swal.fire('Error!', 'There was an error deleting the course.', 'error');
        });
      }
    });
  }

  closeAllModals(): void {
    this.showModal = false;
    this.showAddResourceModal = false;
    this.showResourcesModal = false;
    this.showEditModal = false;
    this.cdr.detectChanges();
  }

  openEditModal(course: Course): void {
    this.selectedCourse = course;
    this.showEditModal = true;
    this.cdr.detectChanges();
  }

  onCourseUpdated(updatedCourse: Course): void {
    const index = this.courses.findIndex(course => course.id === updatedCourse.id);
    if (index !== -1) {
      this.courses[index] = updatedCourse;
      this.filterCourses();
      this.cdr.detectChanges();
    }
  }
}
