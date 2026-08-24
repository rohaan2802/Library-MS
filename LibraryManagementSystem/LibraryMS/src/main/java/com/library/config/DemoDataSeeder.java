package com.library.config;

import com.library.entity.AdminProfile;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Librarian;
import com.library.entity.Student;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.UserRole;
import com.library.repository.AdminProfileRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.LibrarianRepository;
import com.library.repository.StudentRepository;
import com.library.repository.UserRepository;
import com.library.security.PasswordEncryptionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String ADMIN_EMAIL = "admin@demo.libraryms";
    public static final String LIBRARIAN_EMAIL = "librarian@demo.libraryms";
    public static final String STUDENT_EMAIL = "student@demo.libraryms";
    public static final String DEMO_PASSWORD = "DemoLib2026!";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LibrarianRepository librarianRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final PasswordEncryptionService passwordEncryptionService;

    public DemoDataSeeder(
            UserRepository userRepository,
            StudentRepository studentRepository,
            LibrarianRepository librarianRepository,
            AdminProfileRepository adminProfileRepository,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            PasswordEncryptionService passwordEncryptionService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.librarianRepository = librarianRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.passwordEncryptionService = passwordEncryptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo seed skipped — users already exist.");
            return;
        }

        User admin = saveUser("ADM001", "Demo Admin", ADMIN_EMAIL, UserRole.ADMIN);
        AdminProfile adminProfile = new AdminProfile();
        adminProfile.setUser(admin);
        adminProfile.setDepartment("Library Operations");
        adminProfile.setEmployeeId("A-DEMO001");
        adminProfile.setCanManageUsers(true);
        adminProfile.setCanViewReports(true);
        adminProfile.setCanManageCatalog(true);
        adminProfileRepository.save(adminProfile);

        User librarian = saveUser("LIB001", "Demo Librarian", LIBRARIAN_EMAIL, UserRole.LIBRARIAN);
        Librarian librarianProfile = new Librarian();
        librarianProfile.setUser(librarian);
        librarianProfile.setEmployeeId("L-DEMO001");
        librarianProfile.setCanApproveBorrowing(true);
        librarianRepository.save(librarianProfile);

        User student = saveUser("STU001", "Demo Student", STUDENT_EMAIL, UserRole.STUDENT);
        Student studentProfile = new Student();
        studentProfile.setUser(student);
        studentProfile.setStudentId("S-DEMO001");
        studentProfile.setProgram("Computer Science");
        studentProfile.setEnrollmentDate(LocalDate.of(2024, 9, 1));
        studentProfile.setDateOfBirth(LocalDate.of(2004, 1, 15));
        studentProfile.setMaxBorrowLimit(3);
        studentProfile.setCanBorrow(true);
        studentRepository.save(studentProfile);

        seedBook("BK001", "Clean Code", "Robert C. Martin", "Software Engineering", "9780132350884", 2);
        seedBook("BK002", "Designing Data-Intensive Applications", "Martin Kleppmann", "Data Science", "9781449373320", 1);
        seedBook("BK003", "Artificial Intelligence: A Modern Approach", "Stuart Russell", "AI", "9780134610993", 2);

        log.info(
                "Demo accounts ready: {} / {} / {} (password: {})",
                ADMIN_EMAIL,
                LIBRARIAN_EMAIL,
                STUDENT_EMAIL,
                DEMO_PASSWORD);
    }

    private User saveUser(String userId, String name, String email, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncryptionService.encrypt(DEMO_PASSWORD));
        user.setUserRole(role);
        user.setAccountStatus(AccountStatus.active);
        return userRepository.save(user);
    }

    private void seedBook(
            String bookId, String title, String author, String category, String isbn, int copies) {
        Instant now = Instant.now();
        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategory(category);
        book.setIsbn(isbn);
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        book.setFinePerDayPkr(10);
        book.setFinePerDay(BigDecimal.TEN);
        book.setMaxBorrowDays(14);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        book = bookRepository.save(book);

        for (int i = 1; i <= copies; i++) {
            BookCopy copy = new BookCopy();
            copy.setCopyId(UUID.randomUUID().toString());
            copy.setBook(book);
            copy.setIsbnCode(isbn + "-" + i);
            copy.setCopyNumber(i);
            copy.setAvailable(true);
            bookCopyRepository.save(copy);
        }
    }
}
