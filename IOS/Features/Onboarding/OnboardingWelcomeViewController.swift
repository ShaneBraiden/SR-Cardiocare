// OnboardingWelcomeViewController.swift
// SR-Cardiocare — Onboarding Step 1/3: Welcome
// Stitch Screen: 2051c3400f464cffb401939b3c9b7e1f

import UIKit

final class OnboardingWelcomeViewController: UIViewController {

    // MARK: - UI Elements

    private let heroImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFill
        iv.clipsToBounds = true
        iv.layer.cornerRadius = DesignTokens.Radius.xxl
        iv.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.05)
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = NSLocalizedString("onboarding_welcome_title", comment: "")
        label.font = DesignTokens.Typography.lexend(DesignTokens.Typography.hero, weight: .bold)
        label.textColor = DesignTokens.Colors.textMain
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let descriptionLabel: UILabel = {
        let label = UILabel()
        label.text = NSLocalizedString("onboarding_welcome_description", comment: "")
        label.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        label.textColor = DesignTokens.Colors.textSub
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let pageIndicator: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = DesignTokens.Spacing.md
        stack.alignment = .center
        stack.translatesAutoresizingMaskIntoConstraints = false

        // Active dot
        let activeDot = UIView()
        activeDot.backgroundColor = DesignTokens.Colors.primary
        activeDot.layer.cornerRadius = 5
        activeDot.widthAnchor.constraint(equalToConstant: 32).isActive = true
        activeDot.heightAnchor.constraint(equalToConstant: 10).isActive = true

        // Inactive dots
        for _ in 0..<2 {
            let dot = UIView()
            dot.backgroundColor = DesignTokens.Colors.neutralGrey
            dot.layer.cornerRadius = 5
            dot.widthAnchor.constraint(equalToConstant: 10).isActive = true
            dot.heightAnchor.constraint(equalToConstant: 10).isActive = true
            stack.addArrangedSubview(dot)
        }

        stack.insertArrangedSubview(activeDot, at: 0)
        return stack
    }()

    private let nextButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle(NSLocalizedString("onboarding_next_button", comment: ""), for: .normal)
        btn.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        btn.setTitleColor(.white, for: .normal)
        btn.backgroundColor = DesignTokens.Colors.primary
        btn.layer.cornerRadius = DesignTokens.Radius.button
        btn.translatesAutoresizingMaskIntoConstraints = false
        return btn
    }()

    private let skipButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle(NSLocalizedString("onboarding_skip_button", comment: ""), for: .normal)
        btn.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        btn.setTitleColor(DesignTokens.Colors.textSub, for: .normal)
        btn.translatesAutoresizingMaskIntoConstraints = false
        return btn
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupActions()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.surfaceLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        view.addSubviews(skipButton, heroImageView, titleLabel, descriptionLabel, pageIndicator, nextButton)

        NSLayoutConstraint.activate([
            skipButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: DesignTokens.Spacing.sm),
            skipButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            heroImageView.topAnchor.constraint(equalTo: skipButton.bottomAnchor, constant: DesignTokens.Spacing.sm),
            heroImageView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            heroImageView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            heroImageView.heightAnchor.constraint(equalTo: view.heightAnchor, multiplier: 0.4),

            titleLabel.topAnchor.constraint(equalTo: heroImageView.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            descriptionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: DesignTokens.Spacing.base),
            descriptionLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xxl),
            descriptionLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xxl),

            pageIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            pageIndicator.bottomAnchor.constraint(equalTo: nextButton.topAnchor, constant: -DesignTokens.Spacing.xxl),

            nextButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            nextButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            nextButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -DesignTokens.Spacing.xl),
            nextButton.heightAnchor.constraint(equalToConstant: 56),
        ])
    }

    private func setupActions() {
        nextButton.addTarget(self, action: #selector(nextTapped), for: .touchUpInside)
        skipButton.addTarget(self, action: #selector(skipTapped), for: .touchUpInside)
    }

    // MARK: - Actions

    @objc private func nextTapped() {
        let injuryVC = OnboardingInjuryViewController()
        navigationController?.pushViewController(injuryVC, animated: true)
    }

    @objc private func skipTapped() {
        // Skip to patient home
        let homeVC = PatientHomeViewController()
        let nav = UINavigationController(rootViewController: homeVC)
        nav.modalPresentationStyle = .fullScreen
        present(nav, animated: true)
    }
}
