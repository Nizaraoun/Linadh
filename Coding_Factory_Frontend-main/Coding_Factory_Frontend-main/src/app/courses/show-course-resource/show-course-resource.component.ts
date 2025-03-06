import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser'; // Import DomSanitizer
import { Course } from '../../models/courses';
import { CourseResource } from '../../models/CourseResource';
import { CourseResourceService } from '../../services/course-resource.service';

@Component({
  selector: 'app-show-course-resource',
  templateUrl: './show-course-resource.component.html',
  styleUrls: ['./show-course-resource.component.scss']
})
export class ShowCourseResourceComponent implements OnChanges {
  @Input() showModal: boolean = false;
  @Input() selectedCourse: Course | null = null;
  @Output() showModalChange = new EventEmitter<boolean>();

  expandedResource: CourseResource | null = null;
  resources: CourseResource[] = [];

  constructor(
    private courseResourceService: CourseResourceService,
    private cdr: ChangeDetectorRef,
    private sanitizer: DomSanitizer // Inject DomSanitizer
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedCourse'] && this.selectedCourse) {
      this.fetchResources();
    }
  }

  fetchResources(): void {
    if (this.selectedCourse && this.selectedCourse.id) {
      this.courseResourceService.getResourcesForCourse(this.selectedCourse.id).subscribe(
        (resources) => {
          this.resources = [...resources]; // Ensures Angular detects the change
          this.cdr.detectChanges(); // Manually triggers change detection
        },
        (error) => {
          console.error('Error fetching resources:', error);
        }
      );
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.showModalChange.emit(this.showModal);
  }

  toggleResourceDetails(resource: CourseResource): void {
    this.expandedResource = this.expandedResource?.id === resource.id ? null : resource;
  }
  toggleDocumentView(resource: CourseResource): void {
    // Toggle the document display when the "View Document" or "View Document in Large" button is clicked
    this.expandedResource = this.expandedResource?.id === resource.id ? null : resource;
  }
  
  // Method to sanitize and embed PDF/Word docs in iframe
  getDocumentUrl(documentUrl: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(documentUrl);
  }
}
