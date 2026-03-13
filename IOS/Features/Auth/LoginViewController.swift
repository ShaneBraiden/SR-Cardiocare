// LoginViewController.swift
// SR-Cardiocare — Login screen
// Stitch design reference: Uses DesignTokens exclusively.
// Security: Passwords validated client-side, bcrypt on server.
// Tokens stored in Keychain only — never UserDefaults.

import UIKit

final class LoginViewController: UIViewController {

    // MARK: - UI Elements

    private let logoContainerView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
        v.layer.cornerRadius = 32
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let logoImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        // Use SF Symbol as logo — teal medical cross
        let config = UIImage.SymbolConfiguration(pointSize: 40, weight: .bold)
        iv.image = UIImage(systemName: "cross.case.fill", withConfiguration: config)
        iv.tintColor = DesignTokens.Colors.primary
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let appNameLabel: UILabel = {
        let label = UILabel()
        label.text = "SR-Cardiocare"
        label.font = DesignTokens.Typography.lexend(DesignTokens.Typography.title2, weight: .bold)
        label.textColor = DesignTokens.Colors.primary
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = NSLocalizedString("login_title", comment: "")
        label.font = DesignTokens.Typography.inter(DesignTokens.Typography.largeTitle, weight: .bold)
        label.textColor = DesignTokens.Colors.textMain
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let subtitleLabel: UILabel = {
        let label = UILabel()
        label.text = NSLocalizedString("login_subtitle", comment: "")
        label.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        label.textColor = DesignTokens.Colors.textSub
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let emailTextField: UITextField = {
        let tf = UITextField()
        tf.placeholder = NSLocalizedString("login_email_placeholder", comment: "")
        tf.keyboardType = .emailAddress
        tf.autocapitalizationType = .none
        tf.autocorrectionType = .no
        tf.backgroundColor = DesignTokens.Colors.surfaceLight
        tf.layer.cornerRadius = DesignTokens.Radius.input
        tf.layer.borderColor = DesignTokens.Colors.neutralGrey.cgColor
        tf.layer.borderWidth = 1
        tf.leftView = UIView(frame: CGRect(x: 0, y: 0, width: DesignTokens.Spacing.base, height: 0))
        tf.leftViewMode = .always
        tf.translatesAutoresizingMaskIntoConstraints = false
        return tf
    }()

    private let passwordTextField: UITextField = {
        let tf = UITextField()
        tf.placeholder = NSLocalizedString("login_password_placeholder", comment: "")
        tf.isSecureTextEntry = true
        tf.backgroundColor = DesignTokens.Colors.surfaceLight
        tf.layer.cornerRadius = DesignTokens.Radius.input
        tf.layer.borderColor = DesignTokens.Colors.neutralGrey.cgColor
        tf.layer.borderWidth = 1
        tf.leftView = UIView(frame: CGRect(x: 0, y: 0, width: DesignTokens.Spacing.base, height: 0))
        tf.leftViewMode = .always
        tf.translatesAutoresizingMaskIntoConstraints = false
        return tf
    }()

    private let loginButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle(NSLocalizedString("login_button", comment: ""), for: .normal)
        btn.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        btn.setTitleColor(.white, for: .normal)
        btn.backgroundColor = DesignTokens.Colors.primary
        btn.layer.cornerRadius = DesignTokens.Radius.button
        btn.translatesAutoresizingMaskIntoConstraints = false
        return btn
    }()

    private let errorLabel: UILabel = {
        let label = UILabel()
        label.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        label.textColor = DesignTokens.Colors.error
        label.textAlignment = .center
        label.numberOfLines = 0
        label.isHidden = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let activityIndicator: UIActivityIndicatorView = {
        let indicator = UIActivityIndicatorView(style: .medium)
        indicator.hidesWhenStopped = true
        indicator.translatesAutoresizingMaskIntoConstraints = false
        return indicator
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupActions()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        logoContainerView.addSubview(logoImageView)
        view.addSubviews(logoContainerView, appNameLabel, titleLabel, subtitleLabel, emailTextField, passwordTextField, loginButton, errorLabel, activityIndicator)

        NSLayoutConstraint.activate([
            logoContainerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: DesignTokens.Spacing.xxxl),
            logoContainerView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            logoContainerView.widthAnchor.constraint(equalToConstant: 80),
            logoContainerView.heightAnchor.constraint(equalToConstant: 80),

            logoImageView.centerXAnchor.constraint(equalTo: logoContainerView.centerXAnchor),
            logoImageView.centerYAnchor.constraint(equalTo: logoContainerView.centerYAnchor),
            logoImageView.widthAnchor.constraint(equalToConstant: 44),
            logoImageView.heightAnchor.constraint(equalToConstant: 44),

            appNameLabel.topAnchor.constraint(equalTo: logoContainerView.bottomAnchor, constant: DesignTokens.Spacing.base),
            appNameLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            titleLabel.topAnchor.constraint(equalTo: appNameLabel.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: DesignTokens.Spacing.sm),
            subtitleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xxl),
            subtitleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xxl),

            emailTextField.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: DesignTokens.Spacing.xxxl),
            emailTextField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            emailTextField.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            emailTextField.heightAnchor.constraint(equalToConstant: 52),

            passwordTextField.topAnchor.constraint(equalTo: emailTextField.bottomAnchor, constant: DesignTokens.Spacing.base),
            passwordTextField.leadingAnchor.constraint(equalTo: emailTextField.leadingAnchor),
            passwordTextField.trailingAnchor.constraint(equalTo: emailTextField.trailingAnchor),
            passwordTextField.heightAnchor.constraint(equalToConstant: 52),

            errorLabel.topAnchor.constraint(equalTo: passwordTextField.bottomAnchor, constant: DesignTokens.Spacing.sm),
            errorLabel.leadingAnchor.constraint(equalTo: emailTextField.leadingAnchor),
            errorLabel.trailingAnchor.constraint(equalTo: emailTextField.trailingAnchor),

            loginButton.topAnchor.constraint(equalTo: errorLabel.bottomAnchor, constant: DesignTokens.Spacing.xl),
            loginButton.leadingAnchor.constraint(equalTo: emailTextField.leadingAnchor),
            loginButton.trailingAnchor.constraint(equalTo: emailTextField.trailingAnchor),
            loginButton.heightAnchor.constraint(equalToConstant: 56),

            activityIndicator.centerXAnchor.constraint(equalTo: loginButton.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: loginButton.centerYAnchor),
        ])
    }

    private func setupActions() {
        loginButton.addTarget(self, action: #selector(loginTapped), for: .touchUpInside)
    }

    // MARK: - Actions

    @objc private func loginTapped() {
        guard let email = emailTextField.text, !email.isEmpty,
              let password = passwordTextField.text, !password.isEmpty else {
            showError(NSLocalizedString("login_error_empty_fields", comment: ""))
            return
        }

        guard AuthManager.shared.isValidPassword(password) else {
            showError(NSLocalizedString("login_error_invalid_password", comment: ""))
            return
        }

        setLoading(true)

        Task {
            do {
                let user = try await AuthManager.shared.login(email: email, password: password)
                await MainActor.run {
                    setLoading(false)
                    navigateToHome(role: user.role)
                }
            } catch {
                await MainActor.run {
                    setLoading(false)
                    showError(NSLocalizedString("login_error_generic", comment: ""))
                }
            }
        }
    }

    // MARK: - Helpers

    private func setLoading(_ loading: Bool) {
        loginButton.isEnabled = !loading
        loginButton.alpha = loading ? 0.6 : 1.0
        loading ? activityIndicator.startAnimating() : activityIndicator.stopAnimating()
        loginButton.setTitle(loading ? "" : NSLocalizedString("login_button", comment: ""), for: .normal)
    }

    private func showError(_ message: String) {
        errorLabel.text = message
        errorLabel.isHidden = false
    }

    private func navigateToHome(role: UserRole) {
        let vc: UIViewController
        switch role {
        case .doctor:
            vc = DoctorDashboardViewController()
        case .patient:
            // Check if onboarding is completed
            vc = PatientHomeViewController()
        case .admin:
            vc = DoctorDashboardViewController() // TODO: Admin panel
        }
        let nav = UINavigationController(rootViewController: vc)
        nav.modalPresentationStyle = .fullScreen
        present(nav, animated: true)
    }
}
