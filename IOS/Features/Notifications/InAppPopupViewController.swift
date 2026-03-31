// InAppPopupViewController.swift
// SR-Cardiocare — Center modal popup for in-app notifications
// Usage: Present modally with .overCurrentContext and .crossDissolve

import UIKit

final class InAppPopupViewController: UIViewController {

    // MARK: - Properties

    private let popupTitle: String
    private let popupMessage: String
    private let popupIcon: UIImage?
    private let iconTintColor: UIColor
    private let actionTitle: String?
    private let actionHandler: (() -> Void)?

    // MARK: - UI

    private let overlayView: UIView = {
        let v = UIView()
        v.backgroundColor = UIColor.black.withAlphaComponent(0.4)
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let popupCard: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xxl
        v.layer.shadowColor = UIColor.black.cgColor
        v.layer.shadowOpacity = 0.2
        v.layer.shadowRadius = 20
        v.layer.shadowOffset = CGSize(width: 0, height: 10)
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let iconContainer: UIView = {
        let v = UIView()
        v.layer.cornerRadius = 32
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let iconImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let titleLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.numberOfLines = 2
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let messageLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let actionButton: UIButton = {
        let b = UIButton(type: .system)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .semibold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let dismissButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle("Dismiss", for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        b.setTitleColor(DesignTokens.Colors.textSub, for: .normal)
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Init

    init(
        title: String,
        message: String,
        icon: UIImage? = UIImage(systemName: "bell.fill"),
        iconTintColor: UIColor = DesignTokens.Colors.primary,
        actionTitle: String? = nil,
        actionHandler: (() -> Void)? = nil
    ) {
        self.popupTitle = title
        self.popupMessage = message
        self.popupIcon = icon
        self.iconTintColor = iconTintColor
        self.actionTitle = actionTitle
        self.actionHandler = actionHandler
        super.init(nibName: nil, bundle: nil)

        modalPresentationStyle = .overCurrentContext
        modalTransitionStyle = .crossDissolve
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupActions()
        animateIn()
    }

    // MARK: - Setup

    private func setupUI() {
        view.addSubview(overlayView)
        view.addSubview(popupCard)

        iconContainer.backgroundColor = iconTintColor.withAlphaComponent(0.15)
        iconImageView.image = popupIcon
        iconImageView.tintColor = iconTintColor

        titleLabel.text = popupTitle
        messageLabel.text = popupMessage

        iconContainer.addSubview(iconImageView)
        popupCard.addSubviews(iconContainer, titleLabel, messageLabel, dismissButton)

        if let actionTitle = actionTitle {
            actionButton.setTitle(actionTitle, for: .normal)
            popupCard.addSubview(actionButton)
        }

        NSLayoutConstraint.activate([
            overlayView.topAnchor.constraint(equalTo: view.topAnchor),
            overlayView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            overlayView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            overlayView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            popupCard.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            popupCard.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            popupCard.widthAnchor.constraint(equalToConstant: 300),

            iconContainer.topAnchor.constraint(equalTo: popupCard.topAnchor, constant: DesignTokens.Spacing.xl),
            iconContainer.centerXAnchor.constraint(equalTo: popupCard.centerXAnchor),
            iconContainer.widthAnchor.constraint(equalToConstant: 64),
            iconContainer.heightAnchor.constraint(equalToConstant: 64),

            iconImageView.centerXAnchor.constraint(equalTo: iconContainer.centerXAnchor),
            iconImageView.centerYAnchor.constraint(equalTo: iconContainer.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 32),
            iconImageView.heightAnchor.constraint(equalToConstant: 32),

            titleLabel.topAnchor.constraint(equalTo: iconContainer.bottomAnchor, constant: DesignTokens.Spacing.lg),
            titleLabel.leadingAnchor.constraint(equalTo: popupCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            titleLabel.trailingAnchor.constraint(equalTo: popupCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            messageLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: DesignTokens.Spacing.sm),
            messageLabel.leadingAnchor.constraint(equalTo: popupCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            messageLabel.trailingAnchor.constraint(equalTo: popupCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),
        ])

        if actionTitle != nil {
            NSLayoutConstraint.activate([
                actionButton.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: DesignTokens.Spacing.xl),
                actionButton.leadingAnchor.constraint(equalTo: popupCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
                actionButton.trailingAnchor.constraint(equalTo: popupCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),
                actionButton.heightAnchor.constraint(equalToConstant: 48),

                dismissButton.topAnchor.constraint(equalTo: actionButton.bottomAnchor, constant: DesignTokens.Spacing.sm),
                dismissButton.centerXAnchor.constraint(equalTo: popupCard.centerXAnchor),
                dismissButton.bottomAnchor.constraint(equalTo: popupCard.bottomAnchor, constant: -DesignTokens.Spacing.lg),
            ])
        } else {
            NSLayoutConstraint.activate([
                dismissButton.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: DesignTokens.Spacing.xl),
                dismissButton.centerXAnchor.constraint(equalTo: popupCard.centerXAnchor),
                dismissButton.bottomAnchor.constraint(equalTo: popupCard.bottomAnchor, constant: -DesignTokens.Spacing.lg),
            ])
        }

        // Start hidden for animation
        popupCard.alpha = 0
        popupCard.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        overlayView.alpha = 0
    }

    private func setupActions() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(overlayTapped))
        overlayView.addGestureRecognizer(tapGesture)

        dismissButton.addTarget(self, action: #selector(dismissTapped), for: .touchUpInside)
        actionButton.addTarget(self, action: #selector(actionTapped), for: .touchUpInside)
    }

    // MARK: - Animations

    private func animateIn() {
        UIView.animate(withDuration: 0.3, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0.5) {
            self.overlayView.alpha = 1
            self.popupCard.alpha = 1
            self.popupCard.transform = .identity
        }
    }

    private func animateOut(completion: @escaping () -> Void) {
        UIView.animate(withDuration: 0.2, animations: {
            self.overlayView.alpha = 0
            self.popupCard.alpha = 0
            self.popupCard.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        }) { _ in
            completion()
        }
    }

    // MARK: - Actions

    @objc private func overlayTapped() {
        animateOut { [weak self] in
            self?.dismiss(animated: false)
        }
    }

    @objc private func dismissTapped() {
        animateOut { [weak self] in
            self?.dismiss(animated: false)
        }
    }

    @objc private func actionTapped() {
        animateOut { [weak self] in
            self?.dismiss(animated: false) {
                self?.actionHandler?()
            }
        }
    }
}

// MARK: - Convenience Methods

extension UIViewController {

    /// Shows a center modal popup notification
    func showInAppPopup(
        title: String,
        message: String,
        icon: UIImage? = UIImage(systemName: "bell.fill"),
        iconTintColor: UIColor = DesignTokens.Colors.primary,
        actionTitle: String? = nil,
        actionHandler: (() -> Void)? = nil
    ) {
        let popup = InAppPopupViewController(
            title: title,
            message: message,
            icon: icon,
            iconTintColor: iconTintColor,
            actionTitle: actionTitle,
            actionHandler: actionHandler
        )
        present(popup, animated: false)
    }

    /// Shows workout reminder popup
    func showWorkoutReminderPopup(workoutName: String, minutesUntil: Int) {
        let message = minutesUntil == 0
            ? "Your \(workoutName) session starts now!"
            : "Your \(workoutName) session starts in \(minutesUntil) minutes."

        showInAppPopup(
            title: "Workout Reminder",
            message: message,
            icon: UIImage(systemName: "figure.run"),
            iconTintColor: DesignTokens.Colors.success,
            actionTitle: "Start Workout"
        )
    }

    /// Shows activity notification popup
    func showActivityPopup(title: String, message: String) {
        showInAppPopup(
            title: title,
            message: message,
            icon: UIImage(systemName: "person.fill"),
            iconTintColor: DesignTokens.Colors.primary
        )
    }
}
