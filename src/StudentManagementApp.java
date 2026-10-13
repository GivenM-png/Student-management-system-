import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudentManagementApp extends JFrame {
    private static final Color INK = new Color(19, 35, 62);
    private static final Color MUTED = new Color(92, 104, 128);
    private static final Color ACCENT = new Color(54, 90, 220);
    private static final Color BACKGROUND = new Color(241, 244, 249);
    private static final Color PANEL = new Color(255, 255, 255);
    private static final Color SOFT_PANEL = new Color(248, 250, 255);
    private static final Color BORDER = new Color(205, 214, 228);
    private static final Color[] STUDENT_DASHBOARD_COLORS = {
            new Color(20, 145, 139), new Color(100, 91, 214), new Color(226, 151, 47)
    };
    private static final Color[] ADMIN_DASHBOARD_COLORS = {
            new Color(55, 105, 220), new Color(130, 82, 196),
            new Color(28, 155, 119), new Color(225, 129, 73)
    };

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final LinkedHashMap<String, Student> students = new LinkedHashMap<>();
    private final LinkedHashMap<String, CourseModule> courseModules = new LinkedHashMap<>();
    private final LinkedHashMap<String, CourseEnrollment> enrollments = new LinkedHashMap<>();
    private final LinkedHashMap<String, CourseEnrollment> allEnrollments = new LinkedHashMap<>();
    private final LinkedHashMap<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final ApiClient apiClient = new ApiClient();
    private final JTable allStudentsTable = createTable();
    private final JTable searchResultsTable = createTable();
    private final JTable deleteStudentsTable = createTable();
    private final JTable courseCatalogTable = createTable(
            new Object[]{"Faculty", "Course", "Module code", "Module", "Credits", "Fee"});
    private final JTable myEnrollmentsTable = createTable(
            new Object[]{"Faculty", "Course", "Module code", "Module", "Credits", "Fee at enrolment"});
    private final JTable allEnrollmentsTable = createTable(
            new Object[]{"Student email", "Faculty", "Course", "Module code", "Module", "Credits", "Fee"});
    private final JTable applicationsTable = createTable(
            new Object[]{"Email", "First name", "Surname", "Gender", "Date of birth",
                    "Phone", "Home address", "Programme", "Status", "Student ID"});
    private final JTable recentActivityTable = createTable(
            new Object[]{"Actor", "Action", "Entity", "Details"});
    private final JTextField searchField = new JTextField();
    private final JTextField catalogSearchField = new JTextField();
    private final JComboBox<String> facultyFilter = new JComboBox<>();
    private final JTextField updateIdField = new JTextField();
    private final JLabel totalStudentsValue = new JLabel("0");
    private final JLabel totalCoursesValue = new JLabel("0");
    private final JLabel totalModulesValue = new JLabel("0");
    private final JLabel adminAccountsValue = new JLabel("0");
    private final JLabel adminEnrollmentsValue = new JLabel("0");
    private final JLabel enrollmentTotalValue = new JLabel("R 0.00");
    private final JLabel registrationStatusValue = new JLabel("PENDING");
    private final JLabel studentNameValue = new JLabel("-");
    private final JLabel studentSurnameValue = new JLabel("-");
    private final JLabel studentIdValue = new JLabel("Pending");
    private final JLabel studentGenderValue = new JLabel("-");
    private final JLabel studentBirthDateValue = new JLabel("-");
    private final JLabel studentEmailValue = new JLabel("-");
    private final JLabel studentPhoneValue = new JLabel("-");
    private final JLabel studentProgrammeValue = new JLabel("-");
    private final JLabel studentAddressValue = new JLabel("-");
    private final JLabel selectedModulePrice = new JLabel("Select a module to see its fee");
    private final JLabel courseStatus = new JLabel(" ");
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel accountBadge = new JLabel(" ");
    private String currentRole;
    private String selectedCourse;
    private String currentStudentName = "";
    private String currentStudentSurname = "";
    private String currentStudentId = "";
    private JPanel updateForm;
    private JTextField updateName;
    private JTextField updateEmail;
    private JTextField updateCourse;
    private JTextField updatePhone;
    private JLabel updateStatus;

    public StudentManagementApp() {
        super("StudentHub | Student Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 660));
        setSize(1240, 800);
        setLocationRelativeTo(null);
        buildCards();
        setContentPane(buildShell());
        showPage("login");
    }

    private void loadCourseCatalog() {
        courseModules.clear();
        for (CourseModule module : apiClient.getCourseModules()) {
            courseModules.put(module.moduleCode(), module);
        }
        String selected = (String) facultyFilter.getSelectedItem();
        facultyFilter.removeAllItems();
        facultyFilter.addItem("All faculties");
        courseModules.values().stream().map(CourseModule::faculty).distinct().sorted()
                .forEach(facultyFilter::addItem);
        if (selected != null) {
            facultyFilter.setSelectedItem(selected);
        }
        refreshCatalogTable();
    }

    private void buildCards() {
        cardPanel.add(createDashboardPage(false), "dashboard-admin");
        cardPanel.add(createDashboardPage(true), "dashboard-student");
        cardPanel.add(createCourseCatalogPage(), "catalog");
        cardPanel.add(createMyEnrollmentsPage(), "enrollments");
        cardPanel.add(createAllEnrollmentsPage(), "admin-enrollments");
    }

    private JPanel buildShell() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(BACKGROUND);
        workspace.add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(buildTopBar(), BorderLayout.NORTH);
        main.add(cardPanel, BorderLayout.CENTER);
        workspace.add(main, BorderLayout.CENTER);

        JPanel root = new JPanel(new CardLayout());
        JPanel login = new JPanel(new BorderLayout());
        login.add(new LoginPage(apiClient, this::onAuthenticated), BorderLayout.CENTER);
        root.add(login, "login");
        root.add(workspace, "app");
        appRoot = root;
        return root;
    }

    private JPanel appRoot;

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(25, 37, 70));
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBorder(new EmptyBorder(23, 15, 20, 15));

        JLabel logo = new JLabel("  StudentHub");
        logo.setFont(new Font("SansSerif", Font.BOLD, 19));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(42));

        addNavButton(sidebar, "⌂   Dashboard", "dashboard");
        sidebar.add(Box.createVerticalStrut(8));
        JLabel group = new JLabel("STUDENT ADMISSIONS");
        group.setFont(new Font("SansSerif", Font.BOLD, 10));
        group.setForeground(new Color(163, 175, 207));
        group.setBorder(new EmptyBorder(14, 11, 10, 0));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setName("admin-admissions-group");
        sidebar.add(group);
        addNavButton(sidebar, "▣   Review applicants", "review-applications");
        JLabel adminCourseGroup = new JLabel("ACADEMIC OVERSIGHT");
        adminCourseGroup.setName("admin-courses-group");
        adminCourseGroup.setFont(new Font("SansSerif", Font.BOLD, 10));
        adminCourseGroup.setForeground(new Color(163, 175, 207));
        adminCourseGroup.setBorder(new EmptyBorder(18, 11, 10, 0));
        adminCourseGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(adminCourseGroup);
        addNavButton(sidebar, "▤   All enrolments", "admin-enrollments");
        JLabel academicsGroup = new JLabel("COURSE ENROLMENT");
        academicsGroup.setName("student-courses-group");
        academicsGroup.setFont(new Font("SansSerif", Font.BOLD, 10));
        academicsGroup.setForeground(new Color(163, 175, 207));
        academicsGroup.setBorder(new EmptyBorder(18, 11, 10, 0));
        academicsGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(academicsGroup);
        addNavButton(sidebar, "▦   Course catalogue", "catalog");
        addNavButton(sidebar, "✓   My modules", "enrollments");

        sidebar.add(Box.createVerticalGlue());
        JLabel footer = new JLabel("  STUDENT MANAGEMENT");
        footer.setFont(new Font("SansSerif", Font.BOLD, 9));
        footer.setForeground(new Color(163, 175, 207));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(footer);
        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String text, String page) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(new Color(226, 231, 245));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(11, 11, 11, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> showPage(page));
        navigationButtons.put(page, button);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(48, 63, 105));
                button.setOpaque(true);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setOpaque(false);
            }
        });
        sidebar.add(button);
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 234, 242)),
                new EmptyBorder(16, 28, 16, 24)));
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        pageTitle.setForeground(INK);
        top.add(pageTitle, BorderLayout.WEST);

        accountBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        accountBadge.setForeground(new Color(48, 69, 116));
        JPanel account = new JPanel(new BorderLayout(18, 0));
        account.setOpaque(false);
        account.add(accountBadge, BorderLayout.CENTER);
        JButton signOut = new JButton("Sign out");
        styleSecondaryButton(signOut);
        signOut.addActionListener(e -> {
            ApiClient.ApiException logoutError = null;
            try {
                apiClient.logout();
            } catch (ApiClient.ApiException ex) {
                logoutError = ex;
            }
            students.clear();
            showPage("login");
            if (logoutError != null) {
                showMessage(this, "Signed out on this device, but the server session could not be revoked: "
                        + logoutError.getMessage(), JOptionPane.WARNING_MESSAGE);
            }
        });
        top.add(signOut, BorderLayout.EAST);
        top.add(account, BorderLayout.CENTER);
        return top;
    }

    private void showPage(String page) {
        if (page.equals("login")) {
            ((CardLayout) appRoot.getLayout()).show(appRoot, "login");
            return;
        }
        if (!isPageAvailable(page)) {
            showMessage(this, "This workspace does not have access to that section.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (page.equals("review-applications")) {
            page = "dashboard";
        }
        String title = switch (page) {
            case "dashboard" -> "Dashboard";
            case "catalog" -> "Course catalogue";
            case "enrollments" -> "My enrolments";
            case "admin-enrollments" -> "Enrolment oversight";
            default -> "Dashboard";
        };
        pageTitle.setText(title);
        if (page.equals("dashboard")) {
            cards.show(cardPanel, "dashboard-" + (isAdmin() ? "admin" : "student"));
            if (isAdmin()) {
                refreshDashboard();
                refreshApplicationsTable();
            } else {
                refreshStudentDashboard();
            }
            ((CardLayout) appRoot.getLayout()).show(appRoot, "app");
            return;
        } else if (page.equals("catalog")) {
            refreshCatalogTable();
        } else if (page.equals("enrollments")) {
            refreshEnrollmentTable();
        } else if (page.equals("admin-enrollments")) {
            refreshAllEnrollmentTable();
        }
        cards.show(cardPanel, page);
        ((CardLayout) appRoot.getLayout()).show(appRoot, "app");
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole);
    }

    private boolean isPageAvailable(String page) {
        if (currentRole == null || page.equals("dashboard") || page.equals("catalog")) {
            return true;
        }
        if (page.equals("review-applications")) {
            return isAdmin();
        }
        return isAdmin()
                ? !page.equals("enrollments")
                : page.equals("enrollments");
    }

    private void setRoleWorkspace(String name, String role) {
        currentRole = role;
        accountBadge.setText(name + (isAdmin() ? "  |  ADMINISTRATOR" : "  |  STUDENT"));
        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            String page = entry.getKey();
            boolean adminPage = Set.of("admin-enrollments", "review-applications").contains(page);
            entry.getValue().setVisible(isAdmin() == adminPage || page.equals("dashboard")
                    || page.equals("catalog") || page.equals("enrollments") && !isAdmin());
        }
        setSidebarSectionVisible("admin-admissions-group", isAdmin());
        setSidebarSectionVisible("admin-courses-group", isAdmin());
        setSidebarSectionVisible("student-courses-group", !isAdmin());
        if (appRoot != null) {
            appRoot.revalidate();
            appRoot.repaint();
        }
    }

    private void setSidebarSectionVisible(String name, boolean visible) {
        Component[] components = navigationButtons.values().iterator().next().getParent().getComponents();
        for (Component component : components) {
            if (name.equals(component.getName())) {
                component.setVisible(visible);
            }
        }
    }

    private JComponent createDashboardPage(boolean studentView) {
        JPanel content = pageContent();
        content.setBackground(studentView ? new Color(242, 248, 250) : new Color(243, 246, 253));
        content.add(createDashboardHero(studentView));

        if (studentView) {
            JPanel studentProfile = sectionPanel("Student profile", "Name, surname, and student ID");
            studentProfile.setBackground(new Color(235, 245, 250));
            JPanel profileFields = new JPanel(new GridLayout(3, 3, 12, 10));
            profileFields.setOpaque(false);
            profileFields.add(profileDetailCard("First name", studentNameValue, STUDENT_DASHBOARD_COLORS[0]));
            profileFields.add(profileDetailCard("Surname", studentSurnameValue, STUDENT_DASHBOARD_COLORS[0]));
            profileFields.add(profileDetailCard("Student ID", studentIdValue, STUDENT_DASHBOARD_COLORS[1]));
            profileFields.add(profileDetailCard("Gender", studentGenderValue, STUDENT_DASHBOARD_COLORS[2]));
            profileFields.add(profileDetailCard("Date of birth", studentBirthDateValue, STUDENT_DASHBOARD_COLORS[2]));
            profileFields.add(profileDetailCard("Email", studentEmailValue, STUDENT_DASHBOARD_COLORS[1]));
            profileFields.add(profileDetailCard("Phone", studentPhoneValue, STUDENT_DASHBOARD_COLORS[0]));
            profileFields.add(profileDetailCard("Programme of interest", studentProgrammeValue, STUDENT_DASHBOARD_COLORS[1]));
            profileFields.add(profileDetailCard("Home address", studentAddressValue, STUDENT_DASHBOARD_COLORS[2]));
            studentProfile.add(profileFields, BorderLayout.CENTER);
            content.add(studentProfile);
            content.add(Box.createVerticalStrut(18));
        }

        JPanel stats = new JPanel(new GridLayout(studentView ? 1 : 1, studentView ? 3 : 4, 16, 0));
        stats.setOpaque(false);
        if (studentView) {
            stats.add(statCard("MY MODULES", totalStudentsValue, "Modules you have selected",
                    STUDENT_DASHBOARD_COLORS[0]));
            stats.add(statCard("INDICATIVE TOTAL", enrollmentTotalValue, "Sample fees in ZAR",
                    STUDENT_DASHBOARD_COLORS[1]));
            stats.add(statCard("APPLICATION STATUS", registrationStatusValue,
                    "Administrator-reviewed account", STUDENT_DASHBOARD_COLORS[2]));
        } else {
            stats.add(statCard("TOTAL STUDENTS", totalStudentsValue, "Accepted student accounts",
                    ADMIN_DASHBOARD_COLORS[0]));
            stats.add(statCard("TOTAL ACCOUNTS", adminAccountsValue, "Registered user accounts",
                    ADMIN_DASHBOARD_COLORS[1]));
            stats.add(statCard("ENROLMENTS", adminEnrollmentsValue, "Student module selections",
                    ADMIN_DASHBOARD_COLORS[2]));
            stats.add(statCard("TOTAL MODULES", totalModulesValue, "Indicative catalogue entries",
                    ADMIN_DASHBOARD_COLORS[3]));
        }
        content.add(stats);
        content.add(Box.createVerticalStrut(24));

        JPanel quick = sectionPanel(studentView ? "Your next steps" : "Administration tools",
                studentView ? "Choose the action you need" : "Common student and academic administration tasks");
        JPanel actions = new JPanel(new GridLayout(1, studentView ? 2 : 3, 12, 0));
        actions.setOpaque(false);
        if (studentView) {
            actions.add(actionCard("Explore courses", "Compare programmes and module fees", "catalog"));
            actions.add(actionCard("My modules", "Review or withdraw from enrolments", "enrollments"));
        } else {
            actions.add(actionCard("Course catalogue", "Review modules and maintain indicative fees", "catalog"));
            actions.add(actionCard("All enrolments", "Monitor student module selections", "admin-enrollments"));
        }
        quick.add(actions, BorderLayout.CENTER);
        if (!studentView) {
            JPanel exportActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            exportActions.setOpaque(false);
            JButton exportStudents = secondaryButton("Export students CSV");
            JButton exportEnrollments = secondaryButton("Export enrolments CSV");
            exportStudents.addActionListener(e -> exportCsv("students.csv", apiClient.exportStudentsCsv()));
            exportEnrollments.addActionListener(e -> exportCsv("enrollments.csv", apiClient.exportEnrollmentsCsv()));
            exportActions.add(exportStudents);
            exportActions.add(exportEnrollments);
            quick.add(exportActions, BorderLayout.SOUTH);
        }
        content.add(quick);
        if (!studentView) {
            content.add(Box.createVerticalStrut(22));
            content.add(createApplicationReviewPanel());
            content.add(Box.createVerticalStrut(22));
            JPanel activity = sectionPanel("Recent activity", "Latest system events and account actions");
            activity.add(tableScroll(recentActivityTable), BorderLayout.CENTER);
            content.add(Box.createVerticalStrut(24));
            content.add(activity);
        }
        content.add(Box.createVerticalGlue());
        return scrollable(content);
    }

    private JComponent createAllStudentsPage() {
        JPanel content = pageContent();
        content.add(pageHeading("All students", "Browse and manage every student record."));
        JPanel section = sectionPanel("Student directory", "Select a row to update or remove a record");
        section.add(tableScroll(allStudentsTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        JButton edit = primaryButton("Update selected");
        JButton delete = secondaryButton("Delete selected");
        edit.addActionListener(e -> {
            String id = selectedId(allStudentsTable);
            if (id != null) {
                loadStudentForUpdate(id);
                showPage("update");
            }
        });
        delete.addActionListener(e -> deleteSelected(allStudentsTable));
        actions.add(edit);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(delete);
        section.add(actions, BorderLayout.SOUTH);
        content.add(section);
        return scrollable(content);
    }

    private JComponent createSearchPage() {
        JPanel content = pageContent();
        content.add(pageHeading("Search students", "Find a record by student ID, name, email, or course."));
        JPanel section = sectionPanel("Search directory", "Results update as you type");
        searchField.setPreferredSize(new Dimension(320, 40));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 228, 239), 1, true),
                new EmptyBorder(0, 12, 0, 12)));
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        JButton clear = secondaryButton("Clear search");
        clear.addActionListener(e -> searchField.setText(""));
        searchRow.add(clear, BorderLayout.EAST);
        section.add(searchRow, BorderLayout.NORTH);
        section.add(tableScroll(searchResultsTable), BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterSearchResults(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterSearchResults(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterSearchResults(); }
        });
        content.add(section);
        return scrollable(content);
    }

    private JComponent createUpdatePage() {
        JPanel content = pageContent();
        content.add(pageHeading("Update student", "Load a student record by ID, then save your changes."));
        JPanel lookup = sectionPanel("Find student", "Student IDs are case-sensitive");
        updateIdField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        styleInput(updateIdField);
        JPanel lookupRow = new JPanel(new BorderLayout(12, 0));
        lookupRow.setOpaque(false);
        lookupRow.add(labeledField("Student ID", updateIdField), BorderLayout.CENTER);
        JButton load = primaryButton("Load student");
        load.addActionListener(e -> loadStudentForUpdate(updateIdField.getText().trim()));
        lookupRow.add(load, BorderLayout.EAST);
        lookup.add(lookupRow, BorderLayout.NORTH);
        updateStatus = new JLabel(" ");
        updateStatus.setForeground(MUTED);
        updateStatus.setBorder(new EmptyBorder(8, 0, 0, 0));
        lookup.add(updateStatus, BorderLayout.SOUTH);
        content.add(lookup);

        updateForm = sectionPanel("Edit student details", "Update the fields below and save the record");
        updateName = new JTextField();
        updateEmail = new JTextField();
        updateCourse = new JTextField();
        updatePhone = new JTextField();
        JPanel fields = new JPanel(new GridLayout(0, 2, 16, 12));
        fields.setOpaque(false);
        fields.add(labeledField("Full name *", updateName));
        fields.add(labeledField("Email", updateEmail));
        fields.add(labeledField("Course", updateCourse));
        fields.add(labeledField("Phone", updatePhone));
        updateForm.add(fields, BorderLayout.CENTER);
        JButton save = primaryButton("Save changes");
        save.addActionListener(e -> saveStudentUpdate());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(save);
        updateForm.add(actions, BorderLayout.SOUTH);
        updateForm.setVisible(false);
        content.add(updateForm);
        return scrollable(content);
    }

    private JComponent createDeletePage() {
        JPanel content = pageContent();
        content.add(pageHeading("Delete student", "Permanently remove a student record after confirmation."));
        JPanel section = sectionPanel("Student records", "Select a student and review the confirmation prompt");
        section.add(tableScroll(deleteStudentsTable), BorderLayout.CENTER);
        JButton delete = dangerButton("Delete selected student");
        delete.addActionListener(e -> deleteSelected(deleteStudentsTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(delete);
        section.add(actions, BorderLayout.SOUTH);
        content.add(section);
        return scrollable(content);
    }

    private JPanel createApplicationReviewPanel() {
        JPanel section = sectionPanel("Student accounts", "Pending applications appear first");
        section.add(tableScroll(applicationsTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        JButton accept = primaryButton("Accept selected");
        JButton decline = secondaryButton("Decline selected");
        accept.addActionListener(e -> reviewSelectedApplication("ACCEPTED"));
        decline.addActionListener(e -> reviewSelectedApplication("DECLINED"));
        actions.add(accept);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(decline);
        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private JComponent createCourseCatalogPage() {
        JPanel content = pageContent();
        content.add(pageHeading("Course catalogue",
                "Browse programmes and enrol in individual course modules."));

        JPanel notice = new JPanel(new BorderLayout());
        notice.setBackground(new Color(255, 247, 230));
        notice.setBorder(new EmptyBorder(13, 16, 13, 16));
        JLabel noticeText = new JLabel(
                "<html><b>Indicative sample fees in ZAR.</b> These editable examples are not official "
                        + "institutional offerings or confirmed tuition fees. Enrolment here does not process payment.</html>");
        noticeText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noticeText.setForeground(new Color(112, 75, 20));
        notice.add(noticeText, BorderLayout.CENTER);
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(notice);
        content.add(Box.createVerticalStrut(17));

        JPanel section = sectionPanel("Programmes and modules",
                "Select a module to see its fee, then choose Enrol");
        JPanel filters = new JPanel(new GridLayout(1, 2, 12, 0));
        filters.setOpaque(false);
        catalogSearchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        catalogSearchField.setToolTipText("Search faculties, courses, module names or codes");
        catalogSearchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 228, 239), 1, true),
                new EmptyBorder(0, 11, 0, 11)));
        catalogSearchField.setPreferredSize(new Dimension(250, 40));
        facultyFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        facultyFilter.setBackground(Color.WHITE);
        facultyFilter.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        filters.add(catalogSearchField);
        filters.add(facultyFilter);
        JPanel sectionHeader = sectionHeading("Programmes and modules",
                "Select a module to see its fee, then choose Enrol");
        sectionHeader.add(Box.createVerticalStrut(13));
        sectionHeader.add(filters);
        section.add(sectionHeader, BorderLayout.NORTH);

        styleCourseTable(courseCatalogTable);
        section.add(tableScroll(courseCatalogTable), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JPanel feeSummary = new JPanel();
        feeSummary.setOpaque(false);
        feeSummary.setLayout(new BoxLayout(feeSummary, BoxLayout.Y_AXIS));
        JLabel selectedLabel = new JLabel("SELECTED MODULE FEE");
        selectedLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        selectedLabel.setForeground(MUTED);
        selectedModulePrice.setFont(new Font("SansSerif", Font.BOLD, 19));
        selectedModulePrice.setForeground(new Color(35, 116, 83));
        feeSummary.add(selectedLabel);
        feeSummary.add(Box.createVerticalStrut(4));
        feeSummary.add(selectedModulePrice);

        JButton enrol = primaryButton("Enrol in selected module");
        enrol.setPreferredSize(new Dimension(210, 42));
        enrol.addActionListener(e -> enrolSelectedModule());
        enrol.setName("student-enrol-button");
        JButton updateFee = secondaryButton("Update indicative fee");
        updateFee.setPreferredSize(new Dimension(190, 42));
        updateFee.setVisible(false);
        updateFee.setName("admin-price-button");
        updateFee.addActionListener(e -> updateSelectedModuleFee());
        JPanel catalogueActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        catalogueActions.setOpaque(false);
        catalogueActions.add(enrol);
        catalogueActions.add(updateFee);
        footer.add(feeSummary, BorderLayout.CENTER);
        footer.add(catalogueActions, BorderLayout.EAST);
        courseStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        courseStatus.setForeground(MUTED);
        footer.add(courseStatus, BorderLayout.SOUTH);
        footer.setBorder(new EmptyBorder(8, 0, 0, 0));
        section.add(footer, BorderLayout.SOUTH);

        catalogSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshCatalogTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshCatalogTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshCatalogTable(); }
        });
        facultyFilter.addActionListener(e -> refreshCatalogTable());
        courseCatalogTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedModulePrice();
            }
        });
        courseCatalogTable.putClientProperty("updateFeeButton", updateFee);
        courseCatalogTable.putClientProperty("enrolButton", enrol);
        content.add(section);
        return scrollable(content);
    }

    private JComponent createMyEnrollmentsPage() {
        JPanel content = pageContent();
        content.add(pageHeading("My modules",
                "Review modules you have enrolled in and their saved module fees."));
        JPanel notice = new JPanel(new BorderLayout());
        notice.setBackground(new Color(238, 244, 255));
        notice.setBorder(new EmptyBorder(13, 16, 13, 16));
        JLabel noticeText = new JLabel(
                "Fees are indicative ZAR examples only. This system records enrolment; it does not collect payment.");
        noticeText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noticeText.setForeground(new Color(48, 69, 116));
        notice.add(noticeText, BorderLayout.CENTER);
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(notice);
        content.add(Box.createVerticalStrut(17));

        JPanel section = sectionPanel("Enrolled modules",
                "Your module selections and the fee recorded when each enrolment was made");
        styleCourseTable(myEnrollmentsTable);
        section.add(tableScroll(myEnrollmentsTable), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JPanel totalPanel = new JPanel();
        totalPanel.setOpaque(false);
        totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
        JLabel totalCaption = new JLabel("INDICATIVE ENROLMENT TOTAL");
        totalCaption.setFont(new Font("SansSerif", Font.BOLD, 10));
        totalCaption.setForeground(MUTED);
        enrollmentTotalValue.setFont(new Font("SansSerif", Font.BOLD, 21));
        enrollmentTotalValue.setForeground(new Color(35, 116, 83));
        totalPanel.add(totalCaption);
        totalPanel.add(Box.createVerticalStrut(4));
        totalPanel.add(enrollmentTotalValue);

        JButton withdraw = secondaryButton("Withdraw selected");
        withdraw.setPreferredSize(new Dimension(175, 42));
        withdraw.addActionListener(e -> withdrawSelectedModule());
        footer.add(totalPanel, BorderLayout.CENTER);
        footer.add(withdraw, BorderLayout.EAST);
        section.add(footer, BorderLayout.SOUTH);
        content.add(section);
        return scrollable(content);
    }

    private JComponent createAllEnrollmentsPage() {
        JPanel content = pageContent();
        content.add(pageHeading("Enrolment oversight",
                "Review module selections across all student accounts."));
        JPanel notice = new JPanel(new BorderLayout());
        notice.setBackground(new Color(238, 244, 255));
        notice.setBorder(new EmptyBorder(13, 16, 13, 16));
        JLabel noticeText = new JLabel(
                "Administrator view. Displayed fees are the indicative module amounts saved at enrolment.");
        noticeText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noticeText.setForeground(new Color(48, 69, 116));
        notice.add(noticeText, BorderLayout.CENTER);
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(notice);
        content.add(Box.createVerticalStrut(17));

        JPanel section = sectionPanel("Student module enrolments",
                "Student email, selected modules, and saved indicative fees");
        styleCourseTable(allEnrollmentsTable);
        section.add(tableScroll(allEnrollmentsTable), BorderLayout.CENTER);
        content.add(section);
        return scrollable(content);
    }

    private void styleCourseTable(JTable table) {
        table.setRowHeight(39);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(175);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(235);
        table.getColumnModel().getColumn(4).setPreferredWidth(65);
        int feeColumn = table.getColumnCount() - 1;
        table.getColumnModel().getColumn(feeColumn).setPreferredWidth(130);
        table.getColumnModel().getColumn(feeColumn).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(source, value, selected, focused,
                        row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(selected ? source.getSelectionForeground() : new Color(35, 116, 83));
                return cell;
            }
        });
    }

    private void refreshCatalogTable() {
        DefaultTableModel model = (DefaultTableModel) courseCatalogTable.getModel();
        model.setRowCount(0);
        String search = catalogSearchField.getText().trim().toLowerCase(Locale.ROOT);
        Object selectedFaculty = facultyFilter.getSelectedItem();
        String faculty = selectedFaculty == null ? "All faculties" : selectedFaculty.toString();
        for (CourseModule module : courseModules.values()) {
            if (!isAdmin() && selectedCourse != null && !selectedCourse.isBlank()
                    && !module.courseCode().equals(selectedCourse)) {
                continue;
            }
            boolean matchesFaculty = faculty.equals("All faculties") || module.faculty().equals(faculty);
            String searchable = (module.faculty() + " " + module.courseName() + " "
                    + module.courseCode() + " " + module.moduleCode() + " " + module.moduleName())
                    .toLowerCase(Locale.ROOT);
            if (matchesFaculty && searchable.contains(search)) {
                model.addRow(new Object[]{module.faculty(), module.courseName(), module.moduleCode(),
                        module.moduleName(), module.credits(), formatZar(module.priceZar())});
            }
        }
        courseCatalogTable.clearSelection();
        updateSelectedModulePrice();
        JButton updateFee = (JButton) courseCatalogTable.getClientProperty("updateFeeButton");
        JButton enrolButton = (JButton) courseCatalogTable.getClientProperty("enrolButton");
        if (updateFee != null) {
            updateFee.setVisible(isAdmin());
        }
        if (enrolButton != null) {
            enrolButton.setVisible(!isAdmin());
        }
    }

    private void updateSelectedModulePrice() {
        int row = courseCatalogTable.getSelectedRow();
        if (row < 0) {
            selectedModulePrice.setText("Select a module to see its fee");
            return;
        }
        String code = String.valueOf(courseCatalogTable.getValueAt(row, 2));
        CourseModule module = courseModules.get(code);
        selectedModulePrice.setText(module == null
                ? "Select a module to see its fee"
                : formatZar(module.priceZar()));
    }

    private void enrolSelectedModule() {
        int row = courseCatalogTable.getSelectedRow();
        if (row < 0) {
            setCourseStatus("Select a module from the catalogue first.", false);
            return;
        }
        String moduleCode = String.valueOf(courseCatalogTable.getValueAt(row, 2));
        CourseModule module = courseModules.get(moduleCode);
        if (module == null) {
            setCourseStatus("The selected module is no longer available. Refresh the catalogue.", false);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Enrol in " + module.moduleCode() + " — " + module.moduleName()
                        + " for " + formatZar(module.priceZar()) + "?\n"
                        + "This records your enrolment only; no payment will be taken.",
                "Confirm module enrolment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            apiClient.enrollInModule(moduleCode);
            loadMyEnrollments();
            setCourseStatus("Enrolled in " + module.moduleCode() + " successfully.", true);
        } catch (ApiClient.ApiException e) {
            setCourseStatus(e.getMessage(), false);
        }
    }

    private void updateSelectedModuleFee() {
        int row = courseCatalogTable.getSelectedRow();
        if (row < 0) {
            setCourseStatus("Select a module before changing its indicative fee.", false);
            return;
        }
        String moduleCode = String.valueOf(courseCatalogTable.getValueAt(row, 2));
        CourseModule module = courseModules.get(moduleCode);
        if (module == null) {
            setCourseStatus("The selected module is no longer available.", false);
            return;
        }
        String newFee = JOptionPane.showInputDialog(this,
                "Enter the indicative module fee in ZAR (numbers only):",
                module.priceZar().toPlainString());
        if (newFee == null) {
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(newFee.trim());
            if (amount.signum() < 0 || amount.scale() > 2 || amount.precision() > 10) {
                throw new NumberFormatException("The fee must be a non-negative amount with at most 2 decimals.");
            }
        } catch (NumberFormatException e) {
            setCourseStatus("Enter a non-negative ZAR amount with at most two decimal places.", false);
            return;
        }
        try {
            apiClient.updateModulePrice(moduleCode, amount);
            loadCourseCatalog();
            setCourseStatus(moduleCode + " indicative fee updated to " + formatZar(amount) + ".", true);
        } catch (ApiClient.ApiException e) {
            setCourseStatus(e.getMessage(), false);
        }
    }

    private void refreshEnrollmentTable() {
        DefaultTableModel model = (DefaultTableModel) myEnrollmentsTable.getModel();
        model.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (CourseEnrollment enrollment : enrollments.values()) {
            model.addRow(new Object[]{enrollment.faculty(), enrollment.courseName(),
                    enrollment.moduleCode(), enrollment.moduleName(), enrollment.credits(),
                    formatZar(enrollment.priceZar())});
            total = total.add(enrollment.priceZar());
        }
        enrollmentTotalValue.setText(formatZar(total));
    }

    private void refreshAllEnrollmentTable() {
        try {
            allEnrollments.clear();
            for (CourseEnrollment enrollment : apiClient.getAllEnrollments()) {
                allEnrollments.put(enrollment.enrollmentId(), enrollment);
            }
            DefaultTableModel model = (DefaultTableModel) allEnrollmentsTable.getModel();
            model.setRowCount(0);
            for (CourseEnrollment enrollment : allEnrollments.values()) {
                model.addRow(new Object[]{enrollment.accountEmail(), enrollment.faculty(),
                        enrollment.courseName(), enrollment.moduleCode(), enrollment.moduleName(),
                        enrollment.credits(), formatZar(enrollment.priceZar())});
            }
        } catch (ApiClient.ApiException e) {
            showMessage(this, e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void withdrawSelectedModule() {
        int row = myEnrollmentsTable.getSelectedRow();
        if (row < 0) {
            showMessage(this, "Select an enrolled module first.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String moduleCode = String.valueOf(myEnrollmentsTable.getValueAt(row, 2));
        CourseEnrollment selected = enrollments.values().stream()
                .filter(enrollment -> enrollment.moduleCode().equals(moduleCode))
                .findFirst().orElse(null);
        if (selected == null) {
            showMessage(this, "That enrolment is no longer available. Refresh the page.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Withdraw from " + selected.moduleCode() + " — " + selected.moduleName() + "?",
                "Confirm withdrawal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            apiClient.withdrawFromModule(selected.enrollmentId());
            loadMyEnrollments();
        } catch (ApiClient.ApiException e) {
            showMessage(this, e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadMyEnrollments() {
        enrollments.clear();
        for (CourseEnrollment enrollment : apiClient.getMyEnrollments()) {
            enrollments.put(enrollment.enrollmentId(), enrollment);
        }
        refreshEnrollmentTable();
    }

    private void setCourseStatus(String message, boolean success) {
        courseStatus.setText(message);
        courseStatus.setForeground(success ? new Color(35, 116, 83) : new Color(175, 51, 58));
    }

    private String formatZar(BigDecimal amount) {
        return "R " + String.format(Locale.US, "%,.2f", amount);
    }

    private void loadStudentForUpdate(String id) {
        Student student = students.get(id);
        updateIdField.setText(id);
        if (student == null) {
            updateStatus.setText("No student found with that ID.");
            updateStatus.setForeground(new Color(194, 62, 62));
            updateForm.setVisible(false);
            return;
        }
        updateName.setText(student.getName());
        updateEmail.setText(student.getEmail());
        updateCourse.setText(student.getCourse());
        updatePhone.setText(student.getPhone());
        updateStatus.setText("Loaded " + student.getName() + " (" + student.getId() + ").");
        updateStatus.setForeground(new Color(48, 139, 101));
        updateForm.setVisible(true);
        updateForm.revalidate();
    }

    private void saveStudentUpdate() {
        String id = updateIdField.getText().trim();
        Student student = students.get(id);
        String name = updateName.getText().trim();
        if (student == null) {
            updateStatus.setText("Load a valid student record before saving.");
            updateStatus.setForeground(new Color(194, 62, 62));
            return;
        }
        if (name.isEmpty()) {
            showMessage(this, "Full name is required.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Student updated = new Student(id, name, updateEmail.getText().trim(),
                updateCourse.getText().trim(), updatePhone.getText().trim());
        try {
            apiClient.updateStudent(updated);
        } catch (ApiClient.ApiException e) {
            updateStatus.setText(e.getMessage());
            updateStatus.setForeground(new Color(194, 62, 62));
            return;
        }
        student.updateDetails(updated.getName(), updated.getEmail(), updated.getCourse(), updated.getPhone());
        updateStatus.setText("Student record updated.");
        updateStatus.setForeground(new Color(48, 139, 101));
        refreshDashboard();
    }

    private void deleteSelected(JTable table) {
        String id = selectedId(table);
        if (id == null) {
            showMessage(this, "Select a student record first.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Student student = students.get(id);
        if (student == null) {
            showMessage(this, "That student record is no longer available. Refresh your data by signing in again.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + student.getName() + " (" + id + ")? This cannot be undone.",
                "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                apiClient.deleteStudent(id);
                students.remove(id);
            } catch (ApiClient.ApiException e) {
                showMessage(this, e.getMessage(), JOptionPane.ERROR_MESSAGE);
                return;
            }
            fillTable(allStudentsTable, students.values());
            fillTable(deleteStudentsTable, students.values());
            filterSearchResults();
            refreshDashboard();
        }
    }

    private String selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            showMessage(this, "Select a student record first.", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return String.valueOf(table.getValueAt(row, 0));
    }

    private void filterSearchResults() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        fillTable(searchResultsTable, students.values().stream().filter(student ->
                student.getId().toLowerCase(Locale.ROOT).contains(query)
                        || student.getName().toLowerCase(Locale.ROOT).contains(query)
                        || student.getEmail().toLowerCase(Locale.ROOT).contains(query)
                        || student.getCourse().toLowerCase(Locale.ROOT).contains(query)).toList());
    }

    private void refreshApplicationsTable() {
        DefaultTableModel model = (DefaultTableModel) applicationsTable.getModel();
        model.setRowCount(0);
        try {
            for (ApiClient.StudentApplication application : apiClient.getStudentApplications()) {
                model.addRow(new Object[]{
                        application.email(), application.name(), application.surname(), application.gender(),
                        application.birthDate(), application.phone(), application.address(),
                        application.courseInterest(), application.approvalStatus(),
                        application.studentId() == null || application.studentId().isBlank()
                                ? "Not assigned" : application.studentId()
                });
            }
        } catch (ApiClient.ApiException e) {
            showMessage(this, "Could not load student applications: " + e.getMessage(),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String selectedApplicationEmail() {
        int row = applicationsTable.getSelectedRow();
        if (row < 0) {
            showMessage(this, "Select an application first.", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return String.valueOf(applicationsTable.getValueAt(row, 0));
    }

    private void reviewSelectedApplication(String status) {
        String email = selectedApplicationEmail();
        if (email == null) {
            return;
        }
        int row = applicationsTable.getSelectedRow();
        if (!"PENDING".equals(applicationsTable.getValueAt(row, 8))) {
            showMessage(this, "Only pending applications can be reviewed.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String action = "ACCEPTED".equals(status) ? "accepted" : "declined";
        if ("DECLINED".equals(status)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Decline the student application for " + email + "?",
                    "Confirm decline", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            apiClient.updateStudentApplication(email, status);
            refreshApplicationsTable();
            showMessage(this, "Student application " + action + " for " + email + ".",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (ApiClient.ApiException e) {
            showMessage(this, e.getMessage(), JOptionPane.ERROR_MESSAGE);
            refreshApplicationsTable();
        }
    }

    private void selectCourseForStudent() {
        if (courseModules.isEmpty()) {
            return;
        }
        String interest = apiClient.accountCourseInterest().trim();
        selectedCourse = courseModules.values().stream()
                .filter(module -> module.courseCode().equalsIgnoreCase(interest)
                        || module.courseName().equalsIgnoreCase(interest))
                .map(CourseModule::courseCode)
                .findFirst()
                .orElseGet(() -> courseModules.values().stream()
                        .map(CourseModule::courseCode)
                        .sorted()
                        .findFirst()
                        .orElse(""));
    }

    private void refreshDashboard() {
        totalStudentsValue.setText(String.valueOf(students.size()));
        totalCoursesValue.setText(String.valueOf(courseModules.values().stream()
                .map(CourseModule::courseCode).distinct().count()));
        totalModulesValue.setText(String.valueOf(courseModules.size()));
        if (isAdmin()) {
            try {
                ApiClient.AdminOverview overview = apiClient.getAdminOverview();
                adminAccountsValue.setText(String.valueOf(overview.accountCount()));
                adminEnrollmentsValue.setText(String.valueOf(overview.enrollmentCount()));
                totalModulesValue.setText(String.valueOf(overview.moduleCount()));
                totalStudentsValue.setText(String.valueOf(overview.studentCount()));
                refreshRecentActivity(overview.recentActivity());
            } catch (ApiClient.ApiException e) {
                adminAccountsValue.setText("0");
                adminEnrollmentsValue.setText("0");
                refreshRecentActivity(java.util.List.of());
            }
        }
    }

    private void refreshRecentActivity(java.util.List<ApiClient.RecentActivity> entries) {
        DefaultTableModel model = (DefaultTableModel) recentActivityTable.getModel();
        model.setRowCount(0);
        if (entries == null) {
            return;
        }
        for (ApiClient.RecentActivity entry : entries) {
            model.addRow(new Object[]{
                    entry.actorEmail(),
                    entry.action(),
                    entry.entityType(),
                    entry.details()
            });
        }
    }

    private void refreshStudentDashboard() {
        registrationStatusValue.setText(displayValue(apiClient.accountApprovalStatus(), "Unknown"));
        currentStudentName = apiClient.accountName();
        currentStudentSurname = apiClient.accountSurname();
        currentStudentId = apiClient.accountStudentId() == null ? "" : apiClient.accountStudentId();
        studentNameValue.setText(displayValue(currentStudentName));
        studentSurnameValue.setText(displayValue(currentStudentSurname));
        studentIdValue.setText(displayValue(currentStudentId, "Pending"));
        studentGenderValue.setText(displayValue(apiClient.accountGender()));
        studentBirthDateValue.setText(displayValue(apiClient.accountBirthDate()));
        studentEmailValue.setText(displayValue(apiClient.accountEmail()));
        studentPhoneValue.setText(displayValue(apiClient.accountPhone()));
        studentProgrammeValue.setText(displayValue(apiClient.accountCourseInterest()));
        studentAddressValue.setText(displayValue(apiClient.accountAddress()));
        if (selectedCourse == null || selectedCourse.isBlank()) {
            selectedCourse = courseModules.values().stream()
                    .map(CourseModule::courseCode)
                    .distinct()
                    .findFirst().orElse("");
        }
        totalStudentsValue.setText(String.valueOf(enrollments.values().stream()
                .filter(item -> selectedCourse.isBlank() || item.courseCode().equals(selectedCourse))
                .count()));
        BigDecimal total = enrollments.values().stream()
                .filter(item -> selectedCourse.isBlank() || item.courseCode().equals(selectedCourse))
                .map(CourseEnrollment::priceZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        enrollmentTotalValue.setText(formatZar(total));
    }

    private String displayValue(String value) {
        return displayValue(value, "-");
    }

    private String displayValue(String value, String emptyValue) {
        return value == null || value.isBlank() ? emptyValue : value;
    }

    private void onAuthenticated() {
        try {
            setRoleWorkspace((apiClient.accountName() + " " + apiClient.accountSurname()).trim(),
                    apiClient.accountRole());
            if (isAdmin()) {
                allEnrollments.clear();
            } else {
                students.clear();
                allEnrollments.clear();
            }
            loadCourseCatalog();
            if (!isAdmin() && (selectedCourse == null || selectedCourse.isBlank())) {
                selectCourseForStudent();
            }
            loadMyEnrollments();
            refreshDashboard();
            showPage("dashboard");
        } catch (ApiClient.ApiException e) {
            apiClient.clearSession();
            showMessage(this, e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillTable(JTable table, Iterable<Student> records) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Student student : records) {
            model.addRow(new Object[]{student.getId(), student.getName(), student.getEmail(),
                    student.getCourse(), student.getPhone()});
        }
    }

    private JPanel pageContent() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(28, 30, 30, 30));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JScrollPane scrollable(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel pageHeading(String title, String subtitle) {
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
        titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitleLabel);
        heading.add(Box.createVerticalStrut(22));
        return heading;
    }

    private JPanel createDashboardHero(boolean studentView) {
        String firstName = currentStudentName.isBlank() ? "there" : currentStudentName;
        String title = studentView ? "Welcome, " + firstName : "Your campus at a glance";
        String subtitle = studentView
                ? "Your student journey, all in one place. Explore what’s next."
                : "A clear view of student applications, accounts, and academic activity.";
        String eyebrow = studentView ? "STUDENT SPACE  /  PERSONAL DASHBOARD"
                : "STUDENTHUB  /  ADMINISTRATION";
        JPanel hero = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color start = studentView ? new Color(15, 112, 125) : new Color(40, 63, 135);
                Color end = studentView ? new Color(77, 90, 190) : new Color(110, 69, 164);
                g.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(new Color(255, 255, 255, 22));
                g.fillOval(getWidth() - 180, -105, 235, 235);
                g.fillOval(getWidth() - 115, getHeight() - 80, 160, 160);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(26, 30, 26, 26));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrowLabel = new JLabel(eyebrow);
        eyebrowLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrowLabel.setForeground(new Color(221, 230, 255));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        titleLabel.setForeground(Color.WHITE);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(234, 239, 255));
        copy.add(eyebrowLabel);
        copy.add(Box.createVerticalStrut(9));
        copy.add(titleLabel);
        copy.add(Box.createVerticalStrut(6));
        copy.add(subtitleLabel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        hero.add(copy, constraints);

        if (studentView) {
            JLabel monogram = new JLabel(firstName.substring(0, 1).toUpperCase(Locale.ROOT));
            monogram.setFont(new Font("SansSerif", Font.BOLD, 24));
            monogram.setForeground(Color.WHITE);
            monogram.setHorizontalAlignment(SwingConstants.CENTER);
            monogram.setPreferredSize(new Dimension(58, 58));
            monogram.setOpaque(true);
            monogram.setBackground(new Color(255, 255, 255, 45));
            constraints.gridx = 1;
            constraints.weightx = 0;
            constraints.insets = new Insets(0, 20, 0, 0);
            hero.add(monogram, constraints);
        } else {
            JLabel mark = new JLabel("SH");
            mark.setFont(new Font("SansSerif", Font.BOLD, 18));
            mark.setForeground(Color.WHITE);
            mark.setHorizontalAlignment(SwingConstants.CENTER);
            mark.setPreferredSize(new Dimension(58, 58));
            mark.setOpaque(true);
            mark.setBackground(new Color(255, 255, 255, 45));
            constraints.gridx = 1;
            constraints.weightx = 0;
            constraints.insets = new Insets(0, 20, 0, 0);
            hero.add(mark, constraints);
        }
        return hero;
    }

    private JPanel profileDetailCard(String title, JLabel value) {
        return profileDetailCard(title, value, ACCENT);
    }

    private JPanel profileDetailCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel();
        card.setBackground(tint(accent, 0.92f));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(tint(accent, 0.70f), 1, true),
                new EmptyBorder(14, 14, 14, 14)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(accent.darker());
        value.setFont(new Font("SansSerif", Font.BOLD, 15));
        value.setForeground(INK);
        value.setToolTipText(value.getText());
        card.add(label);
        card.add(Box.createVerticalStrut(8));
        card.add(value);
        return card;
    }

    private JPanel statCard(String title, JLabel value, String caption) {
        return statCard(title, value, caption, ACCENT);
    }

    private JPanel statCard(String title, JLabel value, String caption, Color accent) {
        JPanel card = new JPanel();
        card.setBackground(tint(accent, 0.92f));
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(tint(accent, 0.72f), 1, true),
                new EmptyBorder(17, 18, 17, 18)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(accent.darker());
        value.setFont(new Font("SansSerif", Font.BOLD, 28));
        value.setForeground(accent.darker());
        JLabel detail = new JLabel(caption);
        detail.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detail.setForeground(MUTED);
        card.add(label);
        card.add(Box.createVerticalStrut(10));
        card.add(value);
        card.add(Box.createVerticalStrut(4));
        card.add(detail);
        return card;
    }

    private Color tint(Color color, float whiteRatio) {
        return new Color(
                Math.round(color.getRed() * (1 - whiteRatio) + 255 * whiteRatio),
                Math.round(color.getGreen() * (1 - whiteRatio) + 255 * whiteRatio),
                Math.round(color.getBlue() * (1 - whiteRatio) + 255 * whiteRatio));
    }

    private JPanel actionCard(String title, String description, String page) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(SOFT_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 15, 16, 15)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(INK);
        JLabel detail = new JLabel("<html>" + description + "</html>");
        detail.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detail.setForeground(MUTED);
        JButton open = new JButton("Open");
        styleLinkButton(open);
        open.addActionListener(e -> showPage(page));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(open, BorderLayout.SOUTH);
        return card;
    }

    private JPanel sectionPanel(String title, String subtitle) {
        JPanel section = new JPanel(new BorderLayout(0, 15));
        section.setBackground(PANEL);
        section.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionHeading(title, subtitle), BorderLayout.NORTH);
        return section;
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitleLabel);
        return heading;
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel group = new JPanel(new BorderLayout(0, 7));
        group.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(new Font("SansSerif", Font.BOLD, 12));
        caption.setForeground(INK);
        styleInput(field);
        group.add(caption, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(INK);
        field.setPreferredSize(new Dimension(220, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 228, 239), 1, true),
                new EmptyBorder(0, 11, 0, 11)));
    }

    private JTable createTable() {
        return createTable(new Object[]{"Student ID", "Name", "Email", "Course", "Phone"});
    }

    private JTable createTable(Object[] columns) {
        JTable table = new JTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setRowHeight(36);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(INK);
        table.setBackground(PANEL);
        table.setSelectionBackground(new Color(228, 236, 255));
        table.setSelectionForeground(INK);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(218, 226, 238));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(245, 247, 251));
        table.getTableHeader().setOpaque(true);
        return table;
    }

    private JScrollPane tableScroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(new LineBorder(BORDER, 1, true));
        pane.setPreferredSize(new Dimension(700, 350));
        return pane;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(145, 40));
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleSecondaryButton(button);
        button.setPreferredSize(new Dimension(135, 40));
        return button;
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(INK);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JButton dangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(196, 64, 73));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(190, 40));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleLinkButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(ACCENT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void exportCsv(String defaultFileName, String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            showMessage(this, "No CSV data was returned by the server.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV export");
        chooser.setSelectedFile(new File(defaultFileName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path file = chooser.getSelectedFile().toPath();
            if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
                Files.writeString(file, csvContent, StandardCharsets.UTF_8);
            } else {
                Files.writeString(file.resolveSibling(file.getFileName() + ".csv"), csvContent, StandardCharsets.UTF_8);
            }
            showMessage(this, "CSV export saved successfully.", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showMessage(this, "Could not save the export: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(Component parent, String message, int type) {
        JOptionPane.showMessageDialog(parent, message, "StudentHub", type);
    }

    private void clearFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
    }
}
