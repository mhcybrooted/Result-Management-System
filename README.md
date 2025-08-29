# School Exam Result System

A comprehensive Spring Boot application for managing school exam results with **Academic Sessions**, **Multiple Exam Support**, and Thymeleaf web interface.

## Features

- ✅ **Academic Sessions** - Manage multiple school years (2024-25, 2025-26, etc.)
- ✅ **Student Promotion** - Promote students to next class/session
- ✅ **Multiple Exam Types** - Create and manage different exams (Midterm, Final, Quiz, etc.)
- ✅ **Session-aware Operations** - All data filtered by active academic session
- ✅ **Enhanced Mark Entry** - Add marks for specific student-subject-exam combinations
- ✅ **Exam-wise Results** - View results grouped by exam type with detailed breakdown
- ✅ Auto-calculate percentage and grades
- ✅ Simple, clean web interface
- ✅ H2 in-memory database (with PostgreSQL option)
- ✅ Duplicate prevention - Same student can't have duplicate marks for same subject+exam

## Quick Start

### Prerequisites
- Java 21 or higher
- No database setup required (uses H2 by default)

### Running the Application

1. **Clone/Download the project**

2. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

3. **Access the application:**
   - Open browser: http://localhost:8080
   - H2 Console (optional): http://localhost:8080/h2-console

## Sample Data

The application comes with pre-loaded sample data for **2024-25 session**:

### Academic Sessions:
- **2024-25** (Active) - April 2024 to March 2025
- **2025-26** (Inactive) - April 2025 to March 2026

### Students (2024-25 Session):
- John Doe (Roll: 101, Class 10)
- Jane Smith (Roll: 102, Class 10)  
- Mike Johnson (Roll: 103, Class 10)
- Sarah Wilson (Roll: 201, Class 9)
- David Brown (Roll: 202, Class 9)

### Subjects:
- Mathematics (Max: 100)
- English (Max: 100)
- Science (Max: 100)

### Exams (2024-25 Session):
- Midterm Exam (2025-01-15)
- Final Exam (2025-03-15)
- Quiz 1 (2025-02-01)

## How to Use

### 1. Manage Academic Sessions
- Go to "Sessions" page
- Add new academic sessions (e.g., 2025-26)
- Activate the session you want to work with
- Only one session can be active at a time

### 2. Manage Exams
- Go to "Exams" page (shows exams for active session)
- Add new exam types (name + date)
- Edit or delete existing exams
- Only active exams appear in mark entry

### 3. Add Marks
- Go to "Add Marks" page
- Select student, subject, and **exam type** (from active session)
- Enter obtained marks
- Click "Add Marks"
- System prevents duplicate entries for same student-subject-exam combination

### 4. View Results
- Go to "View Results" page
- Enter student roll number (e.g., 101, 102, 103, 201, 202)
- Click "Search" to see **exam-wise breakdown** for active session
- Results show each exam separately with overall summary

### 5. Promote Students
- Go to "Promote Students" page
- Select students from current session
- Choose target session (e.g., promote from 2024-25 to 2025-26)
- Students are automatically promoted to next class (Class 10 → Class 11)

## Academic Session Workflow

### Year-end Process:
1. **Create New Session:** Add "2025-26" session
2. **Promote Students:** Bulk promote Class 10 students to Class 11 in new session
3. **Activate New Session:** Switch to "2025-26" as active session
4. **Continue Operations:** Add exams and marks for new session

### Multi-year Data:
```
2024-25 Session:
- John Doe (Roll: 101, Class 10) → Midterm: 85, Final: 92

2025-26 Session:
- John Doe (Roll: 101, Class 11) → Midterm: 88, Final: 94
```

## Enhanced Results Display

Results now show session-aware data:
```
Current Session: 2024-25

John Doe (Roll: 101) - Class 10
┌─────────┬─────────────┬──────────┬─────────┬────────────┐
│ Subject │ Exam        │ Obtained │ Max     │ Percentage │
├─────────┼─────────────┼──────────┼─────────┼────────────┤
│ Math    │ Midterm     │ 85       │ 100     │ 85.0%      │
│ Math    │ Final       │ 92       │ 100     │ 92.0%      │
│ English │ Midterm     │ 90       │ 100     │ 90.0%      │
│ English │ Quiz 1      │ 95       │ 100     │ 95.0%      │
└─────────┴─────────────┴──────────┴─────────┴────────────┘
Overall: 362/400 (90.5%) - Grade: A
```

## Grade System
- A: 90% and above
- B: 75% - 89%
- C: 60% - 74%
- D: 40% - 59%
- F: Below 40%

## Database Configuration

### Default (H2 - No setup required)
The application uses H2 in-memory database by default. No configuration needed.

### PostgreSQL (Optional)
To use PostgreSQL instead:

1. Create database: `school_exam_db`
2. Update `application.properties`:
   ```properties
   # Comment out H2 config and uncomment PostgreSQL:
   spring.datasource.url=jdbc:postgresql://localhost:5432/school_exam_db
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```

## Project Structure

```
src/main/java/mh/cyb/root/rms/
├── entity/          # Student, Subject, Marks, Exam, Session entities
├── repository/      # JPA repositories with session-aware queries
├── service/         # Business logic with session management
├── controller/      # Web controllers with session context
├── dto/             # Result DTO
├── config/          # Data initialization
└── RmsApplication.java

src/main/resources/
├── templates/       # Thymeleaf HTML templates
│   ├── index.html           # Home page with session context
│   ├── sessions.html        # Session management list
│   ├── add-session.html     # Add new session form
│   ├── promote-students.html # Student promotion interface
│   ├── exams.html           # Exam management (session-aware)
│   ├── add-exam.html        # Add/Edit exam form
│   ├── add-marks.html       # Mark entry (session-aware)
│   └── view-results.html    # Results display (session-aware)
└── application.properties
```

## Technology Stack

- **Backend:** Spring Boot 3.5.5, Spring Data JPA
- **Frontend:** Thymeleaf, Bootstrap 5
- **Database:** H2 (default) / PostgreSQL (optional)
- **Build:** Gradle
- **Java:** 21

## API Endpoints

### Session Management
- `GET /sessions` - List all sessions
- `GET /sessions/add` - Add new session form
- `POST /sessions/add` - Submit new session
- `POST /sessions/{id}/activate` - Activate session

### Student Management
- `GET /students/promote` - Student promotion page
- `POST /students/promote` - Promote selected students

### Core Pages (Session-aware)
- `GET /` - Home page with session context
- `GET /add-marks` - Add marks form (active session only)
- `POST /add-marks` - Submit marks
- `GET /view-results` - Search results form (active session)
- `POST /search-results` - Search by roll number

### Exam Management (Session-aware)
- `GET /exams` - List exams for active session
- `GET /exams/add` - Add new exam form
- `POST /exams/add` - Submit new exam (auto-assigned to active session)
- `GET /exams/edit/{id}` - Edit exam form
- `POST /exams/delete/{id}` - Soft delete exam

## New Features in v3.0

### ✅ Academic Sessions Support
- Create and manage multiple academic years
- Session-aware data filtering
- Only one active session at a time
- Historical data preservation

### ✅ Student Promotion System
- Bulk promote students to next session
- Automatic class progression (Class 10 → Class 11)
- Maintain student history across sessions

### ✅ Session Context UI
- Session indicator on all pages
- Quick session switching
- Session-aware navigation

### ✅ Enhanced Data Management
- Session-specific students and exams
- Clean data separation by academic year
- Multi-year result tracking

## Development

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Clean Build
```bash
./gradlew clean build
```

## Troubleshooting

1. **Port 8080 already in use:**
   - Add to `application.properties`: `server.port=8081`

2. **Database connection issues:**
   - Default H2 should work without setup
   - Check PostgreSQL connection if using PostgreSQL

3. **Build issues:**
   - Ensure Java 21+ is installed
   - Run `./gradlew clean build`

4. **No students/exams visible:**
   - Check if correct session is active
   - Go to "Sessions" and activate appropriate session

## Complete Workflow Example

### Initial Setup (2024-25):
1. **System starts** with 2024-25 as active session
2. **Add Exams:** Midterm, Final, Quiz for 2024-25
3. **Add Marks:** Enter marks for students in active session
4. **View Results:** Check student performance for 2024-25

### Year-end Promotion (2024-25 → 2025-26):
1. **Create Session:** Add "2025-26" session
2. **Promote Students:** Select Class 10 students → Promote to Class 11 in 2025-26
3. **Activate Session:** Switch to 2025-26 as active
4. **Continue:** Add new exams and marks for 2025-26

### Multi-year Benefits:
- **Historical Data:** Keep 2024-25 results intact
- **Clean Separation:** 2025-26 operations don't affect old data
- **Student Tracking:** Same student across multiple years
- **Real School Workflow:** Matches actual academic calendar

## License

This is a comprehensive educational project demonstrating Spring Boot, Thymeleaf, and multi-session academic management.
