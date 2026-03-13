// PostWorkoutFeedbackViewController.swift
// SR-Cardiocare — Post-workout feedback modal
// Stitch Screen: b54ed1a3 (21_post_workout_feedback)
// Shows: success header, pain slider (0-10), difficulty rating, notes, submit button

import UIKit

final class PostWorkoutFeedbackViewController: UIViewController {

    // MARK: - Properties

    private var painLevel: Int = 3
    private var difficultyRating: Int = 3 // 1-5 stars

    // MARK: - UI

    private let handleBar: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.neutralDark
        v.layer.cornerRadius = 2.5
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    // Success header
    private let successContainer: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let checkIcon: UIImageView = {
        let iv = UIImageView()
        iv.image = UIImage(systemName: "checkmark.circle.fill")
        iv.tintColor = DesignTokens.Colors.primary
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let successTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("feedback_workout_complete", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let successSub: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("feedback_great_job", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Pain section
    private let painHeader: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("feedback_pain_header", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let painBadge: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        l.textColor = DesignTokens.Colors.primary
        l.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.1)
        l.layer.cornerRadius = 12
        l.clipsToBounds = true
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let painSlider: UISlider = {
        let s = UISlider()
        s.minimumValue = 0
        s.maximumValue = 10
        s.value = 3
        s.minimumTrackTintColor = DesignTokens.Colors.primary
        s.maximumTrackTintColor = DesignTokens.Colors.neutralLight
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    private let painMinLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("no_pain", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let painMaxLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("severe_pain", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .right
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Difficulty section
    private let difficultyHeader: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("feedback_difficulty_header", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let starsStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.spacing = DesignTokens.Spacing.sm
        s.alignment = .center
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Notes
    private let notesHeader: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("feedback_notes_header", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let notesTextView: UITextView = {
        let tv = UITextView()
        tv.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        tv.textColor = DesignTokens.Colors.textMain
        tv.backgroundColor = DesignTokens.Colors.backgroundLight
        tv.layer.cornerRadius = DesignTokens.Radius.xl
        tv.textContainerInset = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    private let submitButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("feedback_submit", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupStars()
        setupActions()
        updatePainBadge()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.surfaceLight

        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false

        let content = UIView()
        content.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(content)

        // Success header
        successContainer.addSubviews(checkIcon, successTitle, successSub)
        NSLayoutConstraint.activate([
            checkIcon.topAnchor.constraint(equalTo: successContainer.topAnchor, constant: 32),
            checkIcon.centerXAnchor.constraint(equalTo: successContainer.centerXAnchor),
            checkIcon.widthAnchor.constraint(equalToConstant: 56),
            checkIcon.heightAnchor.constraint(equalToConstant: 56),

            successTitle.topAnchor.constraint(equalTo: checkIcon.bottomAnchor, constant: DesignTokens.Spacing.base),
            successTitle.centerXAnchor.constraint(equalTo: successContainer.centerXAnchor),

            successSub.topAnchor.constraint(equalTo: successTitle.bottomAnchor, constant: 4),
            successSub.centerXAnchor.constraint(equalTo: successContainer.centerXAnchor),
            successSub.bottomAnchor.constraint(equalTo: successContainer.bottomAnchor, constant: -24),
        ])

        content.addSubviews(
            successContainer,
            painHeader, painBadge, painSlider, painMinLabel, painMaxLabel,
            difficultyHeader, starsStack,
            notesHeader, notesTextView,
            submitButton
        )

        let pad = DesignTokens.Spacing.xl

        NSLayoutConstraint.activate([
            content.topAnchor.constraint(equalTo: scrollView.topAnchor),
            content.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            content.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            content.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            content.widthAnchor.constraint(equalTo: scrollView.widthAnchor),

            successContainer.topAnchor.constraint(equalTo: content.topAnchor),
            successContainer.leadingAnchor.constraint(equalTo: content.leadingAnchor),
            successContainer.trailingAnchor.constraint(equalTo: content.trailingAnchor),

            painHeader.topAnchor.constraint(equalTo: successContainer.bottomAnchor, constant: pad),
            painHeader.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),

            painBadge.centerYAnchor.constraint(equalTo: painHeader.centerYAnchor),
            painBadge.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -pad),
            painBadge.widthAnchor.constraint(equalToConstant: 80),
            painBadge.heightAnchor.constraint(equalToConstant: 28),

            painSlider.topAnchor.constraint(equalTo: painHeader.bottomAnchor, constant: DesignTokens.Spacing.base),
            painSlider.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),
            painSlider.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -pad),

            painMinLabel.topAnchor.constraint(equalTo: painSlider.bottomAnchor, constant: 4),
            painMinLabel.leadingAnchor.constraint(equalTo: painSlider.leadingAnchor),

            painMaxLabel.topAnchor.constraint(equalTo: painSlider.bottomAnchor, constant: 4),
            painMaxLabel.trailingAnchor.constraint(equalTo: painSlider.trailingAnchor),

            difficultyHeader.topAnchor.constraint(equalTo: painMinLabel.bottomAnchor, constant: pad),
            difficultyHeader.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),

            starsStack.topAnchor.constraint(equalTo: difficultyHeader.bottomAnchor, constant: DesignTokens.Spacing.base),
            starsStack.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),

            notesHeader.topAnchor.constraint(equalTo: starsStack.bottomAnchor, constant: pad),
            notesHeader.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),

            notesTextView.topAnchor.constraint(equalTo: notesHeader.bottomAnchor, constant: DesignTokens.Spacing.base),
            notesTextView.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),
            notesTextView.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -pad),
            notesTextView.heightAnchor.constraint(equalToConstant: 100),

            submitButton.topAnchor.constraint(equalTo: notesTextView.bottomAnchor, constant: pad),
            submitButton.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: pad),
            submitButton.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -pad),
            submitButton.heightAnchor.constraint(equalToConstant: 56),
            submitButton.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -pad),
        ])

        view.addSubviews(handleBar, scrollView)
        NSLayoutConstraint.activate([
            handleBar.topAnchor.constraint(equalTo: view.topAnchor, constant: 8),
            handleBar.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            handleBar.widthAnchor.constraint(equalToConstant: 36),
            handleBar.heightAnchor.constraint(equalToConstant: 5),

            scrollView.topAnchor.constraint(equalTo: handleBar.bottomAnchor, constant: 8),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    private func setupStars() {
        for i in 1...5 {
            let btn = UIButton(type: .system)
            let config = UIImage.SymbolConfiguration(pointSize: 28)
            btn.setImage(UIImage(systemName: i <= difficultyRating ? "star.fill" : "star", withConfiguration: config), for: .normal)
            btn.tintColor = i <= difficultyRating ? DesignTokens.Colors.primary : DesignTokens.Colors.neutralDark
            btn.tag = i
            btn.addTarget(self, action: #selector(starTapped(_:)), for: .touchUpInside)
            starsStack.addArrangedSubview(btn)
        }
    }

    private func setupActions() {
        painSlider.addTarget(self, action: #selector(painSliderChanged), for: .valueChanged)
        submitButton.addTarget(self, action: #selector(submitTapped), for: .touchUpInside)
    }

    private func updatePainBadge() {
        painBadge.text = "Level: \(painLevel)"
    }

    // MARK: - Actions

    @objc private func painSliderChanged() {
        painLevel = Int(painSlider.value)
        updatePainBadge()
    }

    @objc private func starTapped(_ sender: UIButton) {
        difficultyRating = sender.tag
        let config = UIImage.SymbolConfiguration(pointSize: 28)
        for case let btn as UIButton in starsStack.arrangedSubviews {
            let filled = btn.tag <= difficultyRating
            btn.setImage(UIImage(systemName: filled ? "star.fill" : "star", withConfiguration: config), for: .normal)
            btn.tintColor = filled ? DesignTokens.Colors.primary : DesignTokens.Colors.neutralDark
        }
    }

    @objc private func submitTapped() {
        let feedback = WorkoutFeedback(
            workoutSessionId: "",
            painLevel: painLevel,
            difficultyRating: difficultyRating,
            notes: notesTextView.text,
            completedAt: Date()
        )

        // TODO: Send feedback to API
        _ = feedback

        dismiss(animated: true) {
            // Return to patient home
        }
    }
}
