// WorkoutPlayerViewController.swift
// SR-Cardiocare — Video workout player with exercise instructions
// Stitch Screen: b7eefba8 (22_workout_player)
// Shows: video player area, play/pause/skip controls, exercise details, rep counter

import UIKit
import AVFoundation

final class WorkoutPlayerViewController: UIViewController {

    // MARK: - Properties

    var exerciseName: String = "Knee Flexion"
    var exerciseDetail: String = "3 Sets • 10 Reps"
    private var isPlaying = false
    private var currentSet = 1
    private var totalSets = 3
    private var currentRep = 0
    private var totalReps = 10

    // MARK: - UI

    private let headerView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let backButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "arrow.left"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let headerTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("workout_player_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let moreButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "ellipsis"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // Video area
    private let videoContainer: UIView = {
        let v = UIView()
        v.backgroundColor = .black
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let playOverlayButton: UIButton = {
        let b = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 32, weight: .medium)
        b.setImage(UIImage(systemName: "play.fill", withConfiguration: config), for: .normal)
        b.tintColor = .white
        b.backgroundColor = DesignTokens.Colors.primary.withAlphaComponent(0.9)
        b.layer.cornerRadius = 32
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let progressBar: UIProgressView = {
        let pv = UIProgressView(progressViewStyle: .bar)
        pv.progressTintColor = DesignTokens.Colors.primary
        pv.trackTintColor = UIColor.white.withAlphaComponent(0.2)
        pv.progress = 0.33
        pv.translatesAutoresizingMaskIntoConstraints = false
        return pv
    }()

    private let timeCurrentLabel: UILabel = {
        let l = UILabel()
        l.text = "0:45"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = .white
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let timeTotalLabel: UILabel = {
        let l = UILabel()
        l.text = "2:00"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = .white
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Controls bar
    private let controlsView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let rewindButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "gobackward.10"), for: .normal)
        b.tintColor = DesignTokens.Colors.textSub
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let mainPlayButton: UIButton = {
        let b = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 32, weight: .medium)
        b.setImage(UIImage(systemName: "pause.fill", withConfiguration: config), for: .normal)
        b.tintColor = .white
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = 28
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let forwardButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "goforward.10"), for: .normal)
        b.tintColor = DesignTokens.Colors.textSub
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    // Exercise details
    private let scrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private let exerciseNameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let exerciseDetailLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let setCounterLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.primary
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let instructionsHeader: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("workout_instructions", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let instructionsText: UILabel = {
        let l = UILabel()
        l.text = "1. Lie on your back with legs extended\n2. Slowly bend your knee, sliding your heel toward you\n3. Hold for 5 seconds at maximum bend\n4. Return to starting position\n5. Repeat for the prescribed number of reps"
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        l.textColor = DesignTokens.Colors.textSub
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Bottom action
    private let finishButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("workout_finish_set", comment: ""), for: .normal)
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
        setupActions()
        updateCounterLabels()
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header
        headerView.addSubviews(backButton, headerTitle, moreButton)
        NSLayoutConstraint.activate([
            backButton.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 8),
            backButton.centerYAnchor.constraint(equalTo: headerView.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 44),
            backButton.heightAnchor.constraint(equalToConstant: 44),

            headerTitle.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            headerTitle.centerYAnchor.constraint(equalTo: headerView.centerYAnchor),

            moreButton.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -8),
            moreButton.centerYAnchor.constraint(equalTo: headerView.centerYAnchor),
            moreButton.widthAnchor.constraint(equalToConstant: 44),
            moreButton.heightAnchor.constraint(equalToConstant: 44),
        ])

        // Video overlay
        videoContainer.addSubviews(playOverlayButton, progressBar, timeCurrentLabel, timeTotalLabel)
        NSLayoutConstraint.activate([
            playOverlayButton.centerXAnchor.constraint(equalTo: videoContainer.centerXAnchor),
            playOverlayButton.centerYAnchor.constraint(equalTo: videoContainer.centerYAnchor),
            playOverlayButton.widthAnchor.constraint(equalToConstant: 64),
            playOverlayButton.heightAnchor.constraint(equalToConstant: 64),

            progressBar.leadingAnchor.constraint(equalTo: videoContainer.leadingAnchor, constant: 16),
            progressBar.trailingAnchor.constraint(equalTo: videoContainer.trailingAnchor, constant: -16),
            progressBar.bottomAnchor.constraint(equalTo: videoContainer.bottomAnchor, constant: -32),

            timeCurrentLabel.topAnchor.constraint(equalTo: progressBar.bottomAnchor, constant: 4),
            timeCurrentLabel.leadingAnchor.constraint(equalTo: progressBar.leadingAnchor),

            timeTotalLabel.topAnchor.constraint(equalTo: progressBar.bottomAnchor, constant: 4),
            timeTotalLabel.trailingAnchor.constraint(equalTo: progressBar.trailingAnchor),
        ])

        // Controls
        controlsView.addSubviews(rewindButton, mainPlayButton, forwardButton)
        NSLayoutConstraint.activate([
            mainPlayButton.centerXAnchor.constraint(equalTo: controlsView.centerXAnchor),
            mainPlayButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            mainPlayButton.widthAnchor.constraint(equalToConstant: 56),
            mainPlayButton.heightAnchor.constraint(equalToConstant: 56),

            rewindButton.trailingAnchor.constraint(equalTo: mainPlayButton.leadingAnchor, constant: -32),
            rewindButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            rewindButton.widthAnchor.constraint(equalToConstant: 44),
            rewindButton.heightAnchor.constraint(equalToConstant: 44),

            forwardButton.leadingAnchor.constraint(equalTo: mainPlayButton.trailingAnchor, constant: 32),
            forwardButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            forwardButton.widthAnchor.constraint(equalToConstant: 44),
            forwardButton.heightAnchor.constraint(equalToConstant: 44),
        ])

        // Scroll content
        let contentView = UIView()
        contentView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(contentView)
        contentView.addSubviews(exerciseNameLabel, exerciseDetailLabel, setCounterLabel, instructionsHeader, instructionsText)

        NSLayoutConstraint.activate([
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),

            exerciseNameLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: DesignTokens.Spacing.xl),
            exerciseNameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: DesignTokens.Spacing.xl),

            exerciseDetailLabel.topAnchor.constraint(equalTo: exerciseNameLabel.bottomAnchor, constant: 4),
            exerciseDetailLabel.leadingAnchor.constraint(equalTo: exerciseNameLabel.leadingAnchor),

            setCounterLabel.topAnchor.constraint(equalTo: exerciseNameLabel.topAnchor),
            setCounterLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -DesignTokens.Spacing.xl),

            instructionsHeader.topAnchor.constraint(equalTo: exerciseDetailLabel.bottomAnchor, constant: DesignTokens.Spacing.xxl),
            instructionsHeader.leadingAnchor.constraint(equalTo: exerciseNameLabel.leadingAnchor),

            instructionsText.topAnchor.constraint(equalTo: instructionsHeader.bottomAnchor, constant: DesignTokens.Spacing.base),
            instructionsText.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: DesignTokens.Spacing.xl),
            instructionsText.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            instructionsText.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -DesignTokens.Spacing.xl),
        ])

        // Main layout
        view.addSubviews(headerView, videoContainer, controlsView, scrollView, finishButton)

        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerView.heightAnchor.constraint(equalToConstant: 56),

            videoContainer.topAnchor.constraint(equalTo: headerView.bottomAnchor),
            videoContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            videoContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            videoContainer.heightAnchor.constraint(equalTo: view.widthAnchor, multiplier: 9.0/16.0),

            controlsView.topAnchor.constraint(equalTo: videoContainer.bottomAnchor),
            controlsView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            controlsView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            controlsView.heightAnchor.constraint(equalToConstant: 72),

            scrollView.topAnchor.constraint(equalTo: controlsView.bottomAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: finishButton.topAnchor, constant: -DesignTokens.Spacing.base),

            finishButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: DesignTokens.Spacing.xl),
            finishButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            finishButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -DesignTokens.Spacing.base),
            finishButton.heightAnchor.constraint(equalToConstant: 56),
        ])

        exerciseNameLabel.text = exerciseName
        exerciseDetailLabel.text = exerciseDetail
    }

    private func setupActions() {
        backButton.addTarget(self, action: #selector(backTapped), for: .touchUpInside)
        mainPlayButton.addTarget(self, action: #selector(togglePlay), for: .touchUpInside)
        playOverlayButton.addTarget(self, action: #selector(togglePlay), for: .touchUpInside)
        finishButton.addTarget(self, action: #selector(finishSetTapped), for: .touchUpInside)
    }

    private func updateCounterLabels() {
        setCounterLabel.text = "Set \(currentSet)/\(totalSets)"
    }

    // MARK: - Actions

    @objc private func backTapped() {
        navigationController?.popViewController(animated: true)
    }

    @objc private func togglePlay() {
        isPlaying.toggle()
        let config = UIImage.SymbolConfiguration(pointSize: 32, weight: .medium)
        let icon = isPlaying ? "pause.fill" : "play.fill"
        mainPlayButton.setImage(UIImage(systemName: icon, withConfiguration: config), for: .normal)
        playOverlayButton.alpha = isPlaying ? 0 : 1
    }

    @objc private func finishSetTapped() {
        if currentSet < totalSets {
            currentSet += 1
            updateCounterLabels()
        } else {
            // All sets done — show feedback
            let feedbackVC = PostWorkoutFeedbackViewController()
            feedbackVC.modalPresentationStyle = .pageSheet
            present(feedbackVC, animated: true)
        }
    }
}
