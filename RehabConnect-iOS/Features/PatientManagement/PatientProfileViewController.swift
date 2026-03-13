// PatientProfileViewController.swift
// SR-Cardiocare — Patient Profile (Doctor view)
// Stitch Screens: 7e0d2c30 (15_patient_profile_v1), a38284b6 (18_patient_profile_v2)
// Shows: patient info header, progress stats, exercise plan, appointment history

import UIKit

final class PatientProfileViewController: UIViewController {

    // MARK: - UI

    private let backButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "arrow.left"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let moreButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "ellipsis"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let scrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.showsVerticalScrollIndicator = false
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private let contentStack: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.spacing = DesignTokens.Spacing.xl
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Patient card
    private let profileCard: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xxl
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let avatarView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.2)
        v.layer.cornerRadius = 36
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let avatarLabel: UILabel = {
        let l = UILabel()
        l.text = "SW"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.primary
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let patientNameLabel: UILabel = {
        let l = UILabel()
        l.text = "Sarah Wilson"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let conditionLabel: UILabel = {
        let l = UILabel()
        l.text = "ACL Recovery • Week 4"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Stats bar
    private let statsStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.distribution = .fillEqually
        s.spacing = DesignTokens.Spacing.base
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Action buttons
    private let sendFeedbackButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("send_feedback", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let editPlanButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("edit_plan", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        b.setTitleColor(DesignTokens.Colors.primary, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        backButton.addTarget(self, action: #selector(backTapped), for: .touchUpInside)
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header bar
        let headerBar = UIView()
        headerBar.translatesAutoresizingMaskIntoConstraints = false
        headerBar.addSubviews(backButton, moreButton)

        // Profile card content
        profileCard.addSubviews(avatarView, patientNameLabel, conditionLabel)
        avatarView.addSubview(avatarLabel)

        NSLayoutConstraint.activate([
            avatarView.topAnchor.constraint(equalTo: profileCard.topAnchor, constant: 20),
            avatarView.centerXAnchor.constraint(equalTo: profileCard.centerXAnchor),
            avatarView.widthAnchor.constraint(equalToConstant: 72),
            avatarView.heightAnchor.constraint(equalToConstant: 72),

            avatarLabel.centerXAnchor.constraint(equalTo: avatarView.centerXAnchor),
            avatarLabel.centerYAnchor.constraint(equalTo: avatarView.centerYAnchor),

            patientNameLabel.topAnchor.constraint(equalTo: avatarView.bottomAnchor, constant: 12),
            patientNameLabel.centerXAnchor.constraint(equalTo: profileCard.centerXAnchor),

            conditionLabel.topAnchor.constraint(equalTo: patientNameLabel.bottomAnchor, constant: 4),
            conditionLabel.centerXAnchor.constraint(equalTo: profileCard.centerXAnchor),
            conditionLabel.bottomAnchor.constraint(equalTo: profileCard.bottomAnchor, constant: -20),
        ])

        // Stats
        let statItems: [(String, String)] = [
            ("85%", NSLocalizedString("compliance", comment: "")),
            ("3/10", NSLocalizedString("pain_level", comment: "")),
            ("Week 4", NSLocalizedString("progress", comment: "")),
        ]
        for (value, label) in statItems {
            let stat = createStatView(value: value, label: label)
            statsStack.addArrangedSubview(stat)
        }

        // Buttons row
        let buttonStack = UIStackView(arrangedSubviews: [sendFeedbackButton, editPlanButton])
        buttonStack.axis = .horizontal
        buttonStack.spacing = DesignTokens.Spacing.base
        buttonStack.distribution = .fillEqually
        buttonStack.translatesAutoresizingMaskIntoConstraints = false
        sendFeedbackButton.heightAnchor.constraint(equalToConstant: 48).isActive = true

        // Recent exercises section
        let exerciseSection = createSectionView(
            title: NSLocalizedString("recent_exercises", comment: ""),
            items: ["Hamstring Stretch — Completed", "Knee Flexion — In Progress", "Wall Squats — Pending"]
        )

        // Assemble
        let padWrapper = UIView()
        padWrapper.translatesAutoresizingMaskIntoConstraints = false
        padWrapper.addSubview(contentStack)
        NSLayoutConstraint.activate([
            contentStack.topAnchor.constraint(equalTo: padWrapper.topAnchor),
            contentStack.leadingAnchor.constraint(equalTo: padWrapper.leadingAnchor, constant: DesignTokens.Spacing.xl),
            contentStack.trailingAnchor.constraint(equalTo: padWrapper.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            contentStack.bottomAnchor.constraint(equalTo: padWrapper.bottomAnchor),
        ])

        contentStack.addArrangedSubview(profileCard)
        contentStack.addArrangedSubview(statsStack)
        contentStack.addArrangedSubview(buttonStack)
        contentStack.addArrangedSubview(exerciseSection)

        scrollView.addSubview(padWrapper)
        view.addSubviews(headerBar, scrollView)

        NSLayoutConstraint.activate([
            headerBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerBar.heightAnchor.constraint(equalToConstant: 56),

            backButton.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor, constant: 8),
            backButton.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 44),
            backButton.heightAnchor.constraint(equalToConstant: 44),

            moreButton.trailingAnchor.constraint(equalTo: headerBar.trailingAnchor, constant: -8),
            moreButton.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),
            moreButton.widthAnchor.constraint(equalToConstant: 44),
            moreButton.heightAnchor.constraint(equalToConstant: 44),

            scrollView.topAnchor.constraint(equalTo: headerBar.bottomAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            padWrapper.topAnchor.constraint(equalTo: scrollView.topAnchor, constant: DesignTokens.Spacing.base),
            padWrapper.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            padWrapper.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            padWrapper.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor, constant: -DesignTokens.Spacing.xl),
            padWrapper.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
        ])
    }

    // MARK: - Helpers

    private func createStatView(value: String, label: String) -> UIView {
        let container = UIView()
        container.backgroundColor = DesignTokens.Colors.surfaceLight
        container.layer.cornerRadius = DesignTokens.Radius.xl
        container.translatesAutoresizingMaskIntoConstraints = false

        let valueLabel = UILabel()
        valueLabel.text = value
        valueLabel.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        valueLabel.textColor = DesignTokens.Colors.primary
        valueLabel.textAlignment = .center
        valueLabel.translatesAutoresizingMaskIntoConstraints = false

        let descLabel = UILabel()
        descLabel.text = label
        descLabel.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        descLabel.textColor = DesignTokens.Colors.textSub
        descLabel.textAlignment = .center
        descLabel.translatesAutoresizingMaskIntoConstraints = false

        container.addSubviews(valueLabel, descLabel)
        NSLayoutConstraint.activate([
            container.heightAnchor.constraint(equalToConstant: 80),
            valueLabel.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            valueLabel.topAnchor.constraint(equalTo: container.topAnchor, constant: 16),
            descLabel.topAnchor.constraint(equalTo: valueLabel.bottomAnchor, constant: 4),
            descLabel.centerXAnchor.constraint(equalTo: container.centerXAnchor),
        ])
        return container
    }

    private func createSectionView(title: String, items: [String]) -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        titleLabel.textColor = DesignTokens.Colors.textMain
        titleLabel.translatesAutoresizingMaskIntoConstraints = false

        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false

        for item in items {
            let row = UIView()
            row.backgroundColor = DesignTokens.Colors.surfaceLight
            row.layer.cornerRadius = DesignTokens.Radius.lg
            row.translatesAutoresizingMaskIntoConstraints = false
            row.heightAnchor.constraint(equalToConstant: 48).isActive = true

            let label = UILabel()
            label.text = item
            label.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
            label.textColor = DesignTokens.Colors.textMain
            label.translatesAutoresizingMaskIntoConstraints = false

            row.addSubview(label)
            label.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 16).isActive = true
            label.centerYAnchor.constraint(equalTo: row.centerYAnchor).isActive = true

            stack.addArrangedSubview(row)
        }

        container.addSubviews(titleLabel, stack)
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: container.topAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            stack.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 12),
            stack.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor),
        ])

        return container
    }

    // MARK: - Actions

    @objc private func backTapped() {
        navigationController?.popViewController(animated: true)
    }
}
