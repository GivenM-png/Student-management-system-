import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.SpinnerDateModel;

public class LoginPage extends JPanel {
    private static final Color INK = new Color(27, 39, 65);
    private static final Color MUTED = new Color(119, 131, 153);
    private static final Color ACCENT = new Color(76, 104, 243);

    private final JTextField txtField = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton btnLogin = new JButton("Sign in");
    private final JLabel userName = new JLabel("Email address");
    private final JLabel feedback = new JLabel(" ");
    private final ApiClient apiClient;
    private final Runnable onSignIn;
    private JLabel registrationFeedback;
    private String registrationEmail = "";
    private char[] registrationPassword;
    private JTextField registrationEmailInput;
    private JPasswordField registrationPasswordInput;
    private JPasswordField registrationConfirmInput;
    private JTextField registrationName;
    private JTextField registrationSurname;
    private JComboBox<String> registrationGender;
    private final DatePicker registrationBirthDate = new DatePicker();
    private JTextField registrationPhone;
    private JTextField registrationAddress;
    private JComboBox<CourseChoice> registrationCourse;
    private boolean courseOptionsLoaded;
    private JLabel profileFeedback;

    public LoginPage(ApiClient apiClient, Runnable onSignIn) {
        this.apiClient = apiClient;
        this.onSignIn = onSignIn;
        setLayout(new BorderLayout());
        add(createContent(), BorderLayout.CENTER);
    }

    private JPanel createContent() {
        JPanel pages = new JPanel(new CardLayout());
        pages.add(createAuthPage(createLoginPanel()), "login");
        pages.add(createAuthPage(createRegistrationPanel()), "register");
        pages.add(createAuthPage(createProfilePanel()), "profile");
        authCards = (CardLayout) pages.getLayout();
        authPages = pages;
        return pages;
    }

    private CardLayout authCards;
    private JPanel authPages;

    private JPanel createAuthPage(JPanel form) {
        JPanel page = new JPanel(new GridLayout(1, 2));
        page.setBackground(Color.WHITE);
        page.add(new BrandPanel());
        page.add(form);
        return page;
    }

    private JPanel createLoginPanel() {
        JPanel form = createAuthForm("Welcome back", "Sign in to continue to your account.");

        form.add(Box.createVerticalStrut(35));

        userName.setFont(new Font("SansSerif", Font.BOLD, 13));
        userName.setForeground(INK);
        userName.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(userName);
        form.add(Box.createVerticalStrut(9));

        styleField(txtField);
        txtField.setToolTipText("Enter your email address");
        form.add(txtField);
        form.add(Box.createVerticalStrut(21));

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        passwordLabel.setForeground(INK);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordLabel);
        form.add(Box.createVerticalStrut(9));

        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setBackground(Color.WHITE);
        passwordRow.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        passwordRow.setPreferredSize(new Dimension(360, 48));
        passwordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        password.setFont(new Font("SansSerif", Font.PLAIN, 14));
        password.setForeground(INK);
        password.setBorder(new EmptyBorder(0, 13, 0, 4));
        password.setOpaque(false);
        password.setToolTipText("Enter your password");
        passwordRow.add(password, BorderLayout.CENTER);

        JToggleButton showPassword = new JToggleButton("Show");
        showPassword.setFont(new Font("SansSerif", Font.BOLD, 11));
        showPassword.setForeground(ACCENT);
        showPassword.setBorderPainted(false);
        showPassword.setContentAreaFilled(false);
        showPassword.setFocusPainted(false);
        showPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassword.setMargin(new Insets(0, 5, 0, 13));
        char defaultEchoChar = password.getEchoChar();
        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                password.setEchoChar((char) 0);
                showPassword.setText("Hide");
            } else {
                password.setEchoChar(defaultEchoChar);
                showPassword.setText("Show");
            }
        });
        passwordRow.add(showPassword, BorderLayout.EAST);
        addFocusBorder(password, passwordRow);
        form.add(passwordRow);
        form.add(Box.createVerticalStrut(17));

        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JCheckBox remember = new JCheckBox("Remember me");
        remember.setFont(new Font("SansSerif", Font.PLAIN, 12));
        remember.setForeground(MUTED);
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton forgot = new JButton("Forgot password?");
        forgot.setFont(new Font("SansSerif", Font.BOLD, 12));
        forgot.setForeground(ACCENT);
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setFocusPainted(false);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.setMargin(new Insets(0, 0, 0, 0));
        forgot.addActionListener(e -> showFeedback("Contact your administrator to reset your password."));
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        form.add(options);
        form.add(Box.createVerticalStrut(25));

        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(ACCENT);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        btnLogin.setPreferredSize(new Dimension(360, 49));
        btnLogin.addActionListener(e -> signIn());
        btnLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnLogin.setBackground(new Color(59, 85, 220));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnLogin.setBackground(ACCENT);
            }
        });
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(11));

        feedback.setFont(new Font("SansSerif", Font.PLAIN, 12));
        feedback.setForeground(new Color(194, 62, 62));
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(feedback);
        form.add(Box.createVerticalStrut(15));

        JLabel help = new JLabel("Need access? Contact your school administrator.");
        help.setFont(new Font("SansSerif", Font.PLAIN, 12));
        help.setForeground(MUTED);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(help);
        form.add(Box.createVerticalStrut(8));

        JButton createAccount = new JButton("New here? Create a student account");
        styleAuthLink(createAccount);
        createAccount.addActionListener(e -> showAuthPage("register"));
        form.add(createAccount);

        return wrapAuthForm(form);
    }

    private JPanel createRegistrationPanel() {
        JPanel form = createAuthForm("Create your account", "Start with secure sign-in details.");
        form.add(Box.createVerticalStrut(24));

        registrationEmailInput = new JTextField();
        registrationPasswordInput = new JPasswordField();
        registrationConfirmInput = new JPasswordField();
        addLabeledInput(form, "Email address", registrationEmailInput, "Enter your email address");
        form.add(Box.createVerticalStrut(14));
        addLabeledInput(form, "Password (12+ characters, mixed case + number + symbol)",
                registrationPasswordInput, "Create a strong password");
        form.add(Box.createVerticalStrut(14));
        addLabeledInput(form, "Confirm password", registrationConfirmInput, "Re-enter your password");
        form.add(Box.createVerticalStrut(21));

        JButton continueButton = primaryButton("Continue to personal details");
        continueButton.addActionListener(e -> continueRegistration());
        form.add(continueButton);
        form.add(Box.createVerticalStrut(11));

        registrationFeedback = new JLabel(" ");
        registrationFeedback.setFont(new Font("SansSerif", Font.PLAIN, 12));
        registrationFeedback.setForeground(new Color(194, 62, 62));
        registrationFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(registrationFeedback);
        form.add(Box.createVerticalStrut(12));

        JButton backToLogin = new JButton("Already have an account? Sign in");
        styleAuthLink(backToLogin);
        backToLogin.addActionListener(e -> showAuthPage("login"));
        form.add(backToLogin);

        return wrapAuthForm(form);
    }

    private JPanel createProfilePanel() {
        JPanel form = createAuthForm("Your personal details", "Complete your student profile for administrator review.");
        form.add(Box.createVerticalStrut(20));

        JPanel stepRow = new JPanel(new BorderLayout());
        stepRow.setOpaque(false);
        JLabel step = new JLabel("YOUR APPLICATION     \u2022     STEP 2 OF 2");
        step.setFont(new Font("SansSerif", Font.BOLD, 10));
        step.setForeground(ACCENT);
        stepRow.add(step, BorderLayout.WEST);
        form.add(stepRow);
        form.add(Box.createVerticalStrut(15));

        registrationName = new JTextField();
        registrationSurname = new JTextField();
        registrationGender = new JComboBox<>(new String[]{
                "Select gender", "Female", "Male", "Non-binary", "Prefer not to say"
        });
        registrationPhone = new JTextField();
        registrationAddress = new JTextField();
        registrationCourse = new JComboBox<>();
        registrationCourse.setFont(new Font("SansSerif", Font.PLAIN, 13));
        registrationCourse.setForeground(INK);
        registrationCourse.setBackground(Color.WHITE);
        registrationCourse.setPreferredSize(new Dimension(180, 48));
        registrationCourse.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        registrationCourse.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof CourseChoice choice ? choice.name() : "Select a programme");
                return this;
            }
        });

        JPanel fields = new JPanel(new GridLayout(0, 2, 14, 12));
        fields.setOpaque(false);
        fields.add(labeledInput("First name", registrationName, "Your given name"));
        fields.add(labeledInput("Surname", registrationSurname, "Your family name"));
        fields.add(labeledInput("Gender", registrationGender, "Select an option"));
        fields.add(labeledInput("Date of birth", registrationBirthDate, "Choose your date of birth"));
        fields.add(labeledInput("Phone number", registrationPhone, "Optional"));
        fields.add(labeledInput("Programme of interest", registrationCourse, "Choose a programme"));
        JPanel address = labeledInput("Home address", registrationAddress, "Optional");
        address.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(fields);
        form.add(Box.createVerticalStrut(12));
        form.add(address);
        form.add(Box.createVerticalStrut(8));

        JLabel privacy = new JLabel("<html>Your details are securely submitted to your school "
                + "administrator for application review.</html>");
        privacy.setFont(new Font("SansSerif", Font.PLAIN, 11));
        privacy.setForeground(MUTED);
        privacy.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(privacy);
        form.add(Box.createVerticalStrut(13));

        profileFeedback = new JLabel(" ");
        profileFeedback.setFont(new Font("SansSerif", Font.PLAIN, 12));
        profileFeedback.setForeground(new Color(194, 62, 62));
        profileFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(profileFeedback);

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton back = new JButton("Back");
        back.setFont(new Font("SansSerif", Font.BOLD, 12));
        back.setForeground(INK);
        back.setBackground(Color.WHITE);
        back.setFocusPainted(false);
        back.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> showAuthPage("register"));
        JButton submit = primaryButton("Submit application");
        submit.addActionListener(e -> submitRegistration());
        actions.add(back);
        actions.add(submit);
        form.add(actions);
        return wrapAuthForm(form);
    }

    private JPanel labeledInput(String labelText, JComponent field, String tooltip) {
        JPanel group = new JPanel(new BorderLayout(0, 7));
        group.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(INK);
        if (field instanceof JTextField textField) {
            styleField(textField);
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
            comboBox.setForeground(INK);
            comboBox.setBackground(Color.WHITE);
            comboBox.setPreferredSize(new Dimension(180, 48));
            comboBox.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
        }
        field.setToolTipText(tooltip);
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private JPanel createAuthForm(String titleText, String subtitleText) {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("STUDENT MANAGEMENT");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setForeground(ACCENT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(13));
        form.add(title);
        form.add(Box.createVerticalStrut(8));
        form.add(subtitle);
        return form;
    }

    private JPanel wrapAuthForm(JPanel form) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(new EmptyBorder(42, 54, 38, 54));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        outer.add(form, constraints);
        return outer;
    }

    private void addLabeledInput(JPanel form, String labelText, JComponent field, String tooltip) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(INK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(label);
        form.add(Box.createVerticalStrut(8));
        if (field instanceof JTextField textField) {
            styleField(textField);
        } else if (field instanceof JPasswordField passwordField) {
            stylePasswordField(passwordField, tooltip);
        }
        field.setToolTipText(tooltip);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(field);
    }

    private void styleAuthLink(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(ACCENT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        button.setPreferredSize(new Dimension(360, 49));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(59, 85, 220));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(ACCENT);
            }
        });
        return button;
    }

    private void showAuthPage(String page) {
        if (page.equals("login") && registrationPassword != null) {
            java.util.Arrays.fill(registrationPassword, '\0');
            registrationPassword = null;
        }
        if (page.equals("login") && registrationPasswordInput != null) {
            registrationPasswordInput.setText("");
            registrationConfirmInput.setText("");
        }
        authCards.show(authPages, page);
        if (page.equals("profile")) {
            loadCourseOptions();
        }
    }

    private void loadCourseOptions() {
        if (courseOptionsLoaded) {
            return;
        }
        registrationCourse.removeAllItems();
        registrationCourse.addItem(null);
        try {
            Map<String, CourseChoice> courses = new LinkedHashMap<>();
            for (CourseModule module : apiClient.getAvailableCourseModules()) {
                courses.putIfAbsent(module.courseCode(),
                        new CourseChoice(module.courseCode(), module.courseName()));
            }
            courses.values().stream()
                    .sorted(java.util.Comparator.comparing(CourseChoice::name, String.CASE_INSENSITIVE_ORDER))
                    .forEach(registrationCourse::addItem);
            if (courses.isEmpty()) {
                showProfileFeedback("No programmes are available right now. Please try again later.");
                return;
            }
            courseOptionsLoaded = true;
            profileFeedback.setText(" ");
        } catch (ApiClient.ApiException e) {
            showProfileFeedback("Could not load programmes: " + e.getMessage());
        }
    }

    private void continueRegistration() {
        String address = registrationEmailInput.getText().trim();
        char[] enteredPassword = registrationPasswordInput.getPassword();
        char[] repeatedPassword = registrationConfirmInput.getPassword();
        String problem = null;
        if (address.isEmpty()) {
            problem = "Enter your email address.";
        } else if (!address.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            problem = "Enter a valid email address.";
        } else if (enteredPassword.length < 12) {
            problem = "Your password must be at least 12 characters long.";
        } else if (!hasStrongPassword(enteredPassword)) {
            problem = "Use upper and lower case letters, a number, and a symbol.";
        } else if (!java.util.Arrays.equals(enteredPassword, repeatedPassword)) {
            problem = "The passwords do not match.";
        }
        java.util.Arrays.fill(repeatedPassword, '\0');

        if (problem != null) {
            java.util.Arrays.fill(enteredPassword, '\0');
            registrationFeedback.setForeground(new Color(194, 62, 62));
            registrationFeedback.setText(problem);
            return;
        }

        if (registrationPassword != null) {
            java.util.Arrays.fill(registrationPassword, '\0');
        }
        registrationEmail = address;
        registrationPassword = enteredPassword;
        registrationFeedback.setForeground(new Color(48, 139, 101));
        registrationFeedback.setText("Next, complete your student profile.");
        profileFeedback.setText(" ");
        showAuthPage("profile");
    }

    private boolean hasStrongPassword(char[] value) {
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;
        for (char character : value) {
            lower |= Character.isLowerCase(character);
            upper |= Character.isUpperCase(character);
            digit |= Character.isDigit(character);
            symbol |= !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
            if (Character.isWhitespace(character)) {
                return false;
            }
        }
        return lower && upper && digit && symbol;
    }

    private void submitRegistration() {
        String firstName = registrationName.getText().trim();
        String surname = registrationSurname.getText().trim();
        String gender = (String) registrationGender.getSelectedItem();
        if (firstName.isBlank() || surname.isBlank()) {
            showProfileFeedback("Enter your first name and surname.");
            return;
        }
        if (gender == null || gender.equals("Select gender")) {
            showProfileFeedback("Select a gender option.");
            return;
        }
        LocalDate birthDate = registrationBirthDate.getDate();
        if (birthDate == null) {
            showProfileFeedback("Choose your date of birth from the calendar.");
            return;
        }
        CourseChoice courseChoice = (CourseChoice) registrationCourse.getSelectedItem();
        if (courseChoice == null) {
            showProfileFeedback("Choose the programme you are interested in.");
            return;
        }
        if (registrationPassword == null || registrationPassword.length == 0) {
            showProfileFeedback("Return to the previous step and confirm your password.");
            return;
        }
        try {
            apiClient.register(firstName, surname, registrationEmail, gender, birthDate.toString(),
                    registrationPhone.getText().trim(), registrationAddress.getText().trim(),
                    courseChoice.name(), registrationPassword);
        } catch (ApiClient.ApiException e) {
            registrationPassword = null;
            showProfileFeedback(e.getMessage());
            return;
        }
        registrationPassword = null;
        registrationEmail = "";
        registrationEmailInput.setText("");
        registrationPasswordInput.setText("");
        registrationConfirmInput.setText("");
        registrationName.setText("");
        registrationSurname.setText("");
        registrationGender.setSelectedIndex(0);
        registrationBirthDate.setDate(null);
        registrationPhone.setText("");
        registrationAddress.setText("");
        registrationCourse.setSelectedIndex(0);
        password.setText("");
        showFeedback("Application submitted. Sign in after an administrator accepts it.");
        showAuthPage("login");
    }

    private void showProfileFeedback(String message) {
        profileFeedback.setForeground(new Color(194, 62, 62));
        profileFeedback.setText(message);
    }

    private record CourseChoice(String code, String name) {}

    private static class DatePicker extends JPanel {
        private static final DateTimeFormatter DISPLAY_FORMAT =
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault());

        private final JTextField display = new JTextField("Choose a date", 15);
        private final JButton calendarButton = new JButton("\u25a6");
        private final JPopupMenu popup = new JPopupMenu();
        private final JPanel calendar = new JPanel(new BorderLayout(0, 8));
        private LocalDate selectedDate;
        private YearMonth visibleMonth = YearMonth.now().minusYears(18);

        DatePicker() {
            super(new BorderLayout(6, 0));
            setOpaque(false);
            display.setFont(new Font("SansSerif", Font.PLAIN, 14));
            display.setForeground(MUTED);
            display.setEditable(false);
            display.setFocusable(false);
            display.setBackground(Color.WHITE);
            display.setPreferredSize(new Dimension(220, 48));
            display.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(222, 228, 239), 1, true),
                    new EmptyBorder(0, 11, 0, 11)));

            calendarButton.setFont(new Font("SansSerif", Font.BOLD, 17));
            calendarButton.setForeground(ACCENT);
            calendarButton.setBackground(Color.WHITE);
            calendarButton.setFocusPainted(false);
            calendarButton.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
            calendarButton.setPreferredSize(new Dimension(48, 48));
            calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            calendarButton.setToolTipText("Choose date of birth");
            calendarButton.addActionListener(e -> showCalendar());

            add(display, BorderLayout.CENTER);
            add(calendarButton, BorderLayout.EAST);
            setToolTipText("Choose your date of birth from the calendar");
            popup.setBorder(new LineBorder(new Color(205, 214, 228), 1, true));
        }

        LocalDate getDate() {
            return selectedDate;
        }

        void setDate(LocalDate date) {
            selectedDate = date;
            display.setText(date == null ? "Choose a date" : DISPLAY_FORMAT.format(date));
            display.setForeground(date == null ? MUTED : INK);
            visibleMonth = date == null ? YearMonth.now().minusYears(18) : YearMonth.from(date);
        }

        private void showCalendar() {
            renderCalendar();
            popup.show(calendarButton, 0, calendarButton.getHeight());
        }

        private void renderCalendar() {
            calendar.removeAll();
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JButton previous = monthButton("\u2039");
            previous.addActionListener(e -> {
                visibleMonth = visibleMonth.minusMonths(1);
                renderCalendar();
            });
            JButton next = monthButton("\u203a");
            next.setEnabled(visibleMonth.isBefore(YearMonth.from(LocalDate.now())));
            next.addActionListener(e -> {
                YearMonth currentMonth = YearMonth.from(LocalDate.now());
                if (visibleMonth.isBefore(currentMonth)) {
                    visibleMonth = visibleMonth.plusMonths(1);
                    renderCalendar();
                }
            });

            JLabel monthLabel = new JLabel(visibleMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), SwingConstants.CENTER);
            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            monthLabel.setForeground(INK);
            header.add(previous, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            calendar.add(header, BorderLayout.NORTH);

            JPanel days = new JPanel(new GridLayout(7, 7, 3, 3));
            days.setBackground(Color.WHITE);
            for (String day : new String[]{"M", "T", "W", "T", "F", "S", "S"}) {
                JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
                dayLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
                dayLabel.setForeground(MUTED);
                dayLabel.setOpaque(true);
                dayLabel.setBackground(new Color(245, 247, 251));
                days.add(dayLabel);
            }
            LocalDate firstDay = visibleMonth.atDay(1);
            int offset = firstDay.getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) {
                days.add(new JLabel());
            }
            LocalDate today = LocalDate.now();
            for (int day = 1; day <= visibleMonth.lengthOfMonth(); day++) {
                LocalDate date = visibleMonth.atDay(day);
                JLabel dayCell = new JLabel(Integer.toString(day), SwingConstants.CENTER);
                dayCell.setFont(new Font("SansSerif", Font.BOLD, 12));
                dayCell.setOpaque(true);
                dayCell.setPreferredSize(new Dimension(34, 30));
                boolean unavailable = date.isAfter(today);
                dayCell.setForeground(date.equals(selectedDate) ? Color.WHITE
                        : unavailable ? new Color(170, 178, 192) : INK);
                dayCell.setBackground(date.equals(selectedDate) ? ACCENT : Color.WHITE);
                if (!unavailable) {
                    dayCell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    dayCell.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            if (!date.equals(selectedDate)) {
                                dayCell.setBackground(new Color(232, 237, 255));
                            }
                        }

                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            dayCell.setBackground(date.equals(selectedDate) ? ACCENT : Color.WHITE);
                        }

                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            setDate(date);
                            popup.setVisible(false);
                        }
                    });
                } else {
                    dayCell.setToolTipText("Future dates cannot be selected");
                }
                days.add(dayCell);
            }
            while (days.getComponentCount() < 49) {
                days.add(new JLabel());
            }
            days.setPreferredSize(new Dimension(266, 231));
            calendar.add(days, BorderLayout.CENTER);

            JButton clear = new JButton("Clear date");
            clear.setFont(new Font("SansSerif", Font.PLAIN, 11));
            clear.setForeground(ACCENT);
            clear.setBorderPainted(false);
            clear.setContentAreaFilled(false);
            clear.setFocusPainted(false);
            clear.setHorizontalAlignment(SwingConstants.LEFT);
            clear.addActionListener(e -> {
                setDate(null);
                popup.setVisible(false);
            });
            calendar.add(clear, BorderLayout.SOUTH);
            calendar.setBorder(new EmptyBorder(10, 10, 8, 10));
            popup.removeAll();
            popup.add(calendar);
            popup.setPopupSize(310, 315);
            popup.revalidate();
            popup.repaint();
        }

        private JButton monthButton(String text) {
            JButton button = new JButton(text);
            button.setFont(new Font("SansSerif", Font.BOLD, 20));
            button.setForeground(ACCENT);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(35, 30));
            return button;
        }
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(INK);
        field.setCaretColor(ACCENT);
        field.setBorder(new EmptyBorder(0, 13, 0, 13));
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(360, 48));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 228, 239), 1, true),
                new EmptyBorder(0, 12, 0, 12)));
        addFocusBorder(field, field);
    }

    private void stylePasswordField(JPasswordField field, String tooltip) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(INK);
        field.setCaretColor(ACCENT);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(360, 48));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 228, 239), 1, true),
                new EmptyBorder(0, 12, 0, 12)));
        field.setToolTipText(tooltip);
        addFocusBorder(field, field);
    }

    private void addFocusBorder(JComponent watched, JComponent bordered) {
        watched.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                bordered.setBorder(new LineBorder(new Color(133, 151, 255), 1, true));
            }

            @Override
            public void focusLost(FocusEvent e) {
                bordered.setBorder(new LineBorder(new Color(222, 228, 239), 1, true));
            }
        });
    }

    private void signIn() {
        String username = txtField.getText().trim();
        if (username.isEmpty()) {
            showFeedback("Enter your email address to continue.");
            txtField.requestFocusInWindow();
            return;
        }
        char[] enteredPassword = password.getPassword();
        if (enteredPassword.length == 0) {
            java.util.Arrays.fill(enteredPassword, '\0');
            showFeedback("Enter your password to continue.");
            password.requestFocusInWindow();
            return;
        }
        ApiClient.ApiException error = null;
        try {
            apiClient.login(username, enteredPassword);
        } catch (ApiClient.ApiException e) {
            error = e;
        }
        if (error != null) {
            showFeedback(error.getMessage());
            password.setText("");
            password.requestFocusInWindow();
            return;
        }
        onSignIn.run();
    }

    private void showFeedback(String message) {
        feedback.setText(message);
        feedback.setForeground(new Color(194, 62, 62));
    }

    private static class BrandPanel extends JPanel {
        BrandPanel() {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(48, 48, 42, 42));
            setOpaque(false);

            JPanel copy = new JPanel();
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

            JPanel mark = new JPanel(new GridBagLayout());
            mark.setBackground(new Color(255, 255, 255, 35));
            mark.setPreferredSize(new Dimension(54, 54));
            mark.setMaximumSize(new Dimension(54, 54));
            mark.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel markText = new JLabel("S");
            markText.setFont(new Font("SansSerif", Font.BOLD, 25));
            markText.setForeground(Color.WHITE);
            mark.add(markText);

            JLabel brand = new JLabel("StudentHub");
            brand.setFont(new Font("SansSerif", Font.BOLD, 16));
            brand.setForeground(Color.WHITE);
            brand.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel headline = new JLabel("<html>Every student.<br>Every opportunity.</html>");
            headline.setFont(new Font("SansSerif", Font.BOLD, 34));
            headline.setForeground(Color.WHITE);
            headline.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel description = new JLabel("<html>A simpler way to stay on top of<br>learning, progress, and what's next.</html>");
            description.setFont(new Font("SansSerif", Font.PLAIN, 14));
            description.setForeground(new Color(221, 229, 255));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);

            copy.add(mark);
            copy.add(Box.createVerticalStrut(22));
            copy.add(brand);
            copy.add(Box.createVerticalStrut(54));
            copy.add(headline);
            copy.add(Box.createVerticalStrut(16));
            copy.add(description);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            add(copy, constraints);

            JLabel footer = new JLabel("LEARN  \u2022  GROW  \u2022  SUCCEED");
            footer.setFont(new Font("SansSerif", Font.BOLD, 10));
            footer.setForeground(new Color(198, 211, 255));
            constraints.gridy = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(30, 0, 0, 0);
            add(footer, constraints);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new GradientPaint(0, 0, new Color(39, 56, 112),
                    getWidth(), getHeight(), new Color(74, 86, 190)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 17));
            g.fillOval(getWidth() - 205, -100, 285, 285);
            g.fillOval(getWidth() - 145, getHeight() - 190, 245, 245);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
