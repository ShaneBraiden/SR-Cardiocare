// VideoUploadViewController.swift
// SR-Cardiocare — Video Upload for exercises (Doctor-side)
// Stitch Screens: 01-04, 09, 12, 16-17, 23, 27, 29 (video_upload variants)
// Shows: upload area, video preview, metadata form, progress indicator

import UIKit

final class VideoUploadViewController: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate {

    // MARK: - Properties

    private var selectedVideoURL: URL?

    // MARK: - UI

    private let backButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "arrow.left"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let headerTitle: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("video_upload_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let scrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.showsVerticalScrollIndicator = false
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    // Upload zone
    private let uploadZone: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.backgroundLight
        v.layer.cornerRadius = DesignTokens.Radius.xxl
        v.layer.borderWidth = 2
        v.layer.borderColor = DesignTokens.Colors.primary.withAlphaComponent(0.3).cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let uploadIcon: UIImageView = {
        let iv = UIImageView()
        iv.image = UIImage(systemName: "arrow.up.circle.fill")
        iv.tintColor = DesignTokens.Colors.primary
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let uploadLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("video_upload_tap", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let uploadSubLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("video_upload_formats", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    // Metadata form
    private let exerciseNameField = FormTextField(label: NSLocalizedString("exercise_name", comment: ""), placeholder: "e.g. Knee Flexion", icon: "figure.walk")
    private let categoryField = FormTextField(label: NSLocalizedString("category", comment: ""), placeholder: "e.g. Knee", icon: "tag")
    private let difficultyField = FormTextField(label: NSLocalizedString("difficulty", comment: ""), placeholder: "Beginner / Intermediate / Advanced", icon: "chart.bar")
    private let durationField = FormTextField(label: NSLocalizedString("duration", comment: ""), placeholder: "e.g. 120 (seconds)", icon: "clock")

    private let instructionsLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("instructions", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let instructionsTextView: UITextView = {
        let tv = UITextView()
        tv.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        tv.textColor = DesignTokens.Colors.textMain
        tv.backgroundColor = DesignTokens.Colors.backgroundLight
        tv.layer.cornerRadius = DesignTokens.Radius.xl
        tv.textContainerInset = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    // Progress
    private let progressView: UIProgressView = {
        let pv = UIProgressView(progressViewStyle: .bar)
        pv.progressTintColor = DesignTokens.Colors.primary
        pv.trackTintColor = DesignTokens.Colors.neutralLight
        pv.progress = 0
        pv.isHidden = true
        pv.translatesAutoresizingMaskIntoConstraints = false
        return pv
    }()

    private let progressLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = DesignTokens.Colors.primary
        l.isHidden = true
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let uploadButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("upload_video_button", comment: ""), for: .normal)
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
    }

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.surfaceLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        let headerBar = UIView()
        headerBar.translatesAutoresizingMaskIntoConstraints = false
        headerBar.addSubviews(backButton, headerTitle)

        // Upload zone content
        uploadZone.addSubviews(uploadIcon, uploadLabel, uploadSubLabel)
        NSLayoutConstraint.activate([
            uploadIcon.centerXAnchor.constraint(equalTo: uploadZone.centerXAnchor),
            uploadIcon.topAnchor.constraint(equalTo: uploadZone.topAnchor, constant: 32),
            uploadIcon.widthAnchor.constraint(equalToConstant: 48),
            uploadIcon.heightAnchor.constraint(equalToConstant: 48),
            uploadLabel.topAnchor.constraint(equalTo: uploadIcon.bottomAnchor, constant: 12),
            uploadLabel.centerXAnchor.constraint(equalTo: uploadZone.centerXAnchor),
            uploadSubLabel.topAnchor.constraint(equalTo: uploadLabel.bottomAnchor, constant: 4),
            uploadSubLabel.centerXAnchor.constraint(equalTo: uploadZone.centerXAnchor),
            uploadSubLabel.bottomAnchor.constraint(equalTo: uploadZone.bottomAnchor, constant: -32),
        ])

        // Instructions section
        let instructionsRow = UIView()
        instructionsRow.translatesAutoresizingMaskIntoConstraints = false
        instructionsRow.addSubviews(instructionsLabel, instructionsTextView)
        NSLayoutConstraint.activate([
            instructionsLabel.topAnchor.constraint(equalTo: instructionsRow.topAnchor),
            instructionsLabel.leadingAnchor.constraint(equalTo: instructionsRow.leadingAnchor),
            instructionsTextView.topAnchor.constraint(equalTo: instructionsLabel.bottomAnchor, constant: 8),
            instructionsTextView.leadingAnchor.constraint(equalTo: instructionsRow.leadingAnchor),
            instructionsTextView.trailingAnchor.constraint(equalTo: instructionsRow.trailingAnchor),
            instructionsTextView.heightAnchor.constraint(equalToConstant: 120),
            instructionsTextView.bottomAnchor.constraint(equalTo: instructionsRow.bottomAnchor),
        ])

        let formStack = UIStackView(arrangedSubviews: [
            uploadZone, progressView, progressLabel,
            exerciseNameField, categoryField, difficultyField, durationField,
            instructionsRow, uploadButton,
        ])
        formStack.axis = .vertical
        formStack.spacing = DesignTokens.Spacing.lg
        formStack.translatesAutoresizingMaskIntoConstraints = false
        uploadButton.heightAnchor.constraint(equalToConstant: 56).isActive = true

        scrollView.addSubview(formStack)
        view.addSubviews(headerBar, scrollView)

        let pad = DesignTokens.Spacing.xl
        NSLayoutConstraint.activate([
            headerBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerBar.heightAnchor.constraint(equalToConstant: 56),

            backButton.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor, constant: 8),
            backButton.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 44),
            backButton.heightAnchor.constraint(equalToConstant: 44),
            headerTitle.centerXAnchor.constraint(equalTo: headerBar.centerXAnchor),
            headerTitle.centerYAnchor.constraint(equalTo: headerBar.centerYAnchor),

            scrollView.topAnchor.constraint(equalTo: headerBar.bottomAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            formStack.topAnchor.constraint(equalTo: scrollView.topAnchor, constant: pad),
            formStack.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor, constant: pad),
            formStack.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor, constant: -pad),
            formStack.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor, constant: -pad),
            formStack.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -pad * 2),
        ])
    }

    private func setupActions() {
        backButton.addTarget(self, action: #selector(backTapped), for: .touchUpInside)
        uploadButton.addTarget(self, action: #selector(uploadTapped), for: .touchUpInside)

        let tap = UITapGestureRecognizer(target: self, action: #selector(selectVideo))
        uploadZone.addGestureRecognizer(tap)

        durationField.textField.keyboardType = .numberPad
    }

    @objc private func backTapped() { navigationController?.popViewController(animated: true) }

    @objc private func selectVideo() {
        let picker = UIImagePickerController()
        picker.sourceType = .photoLibrary
        picker.mediaTypes = ["public.movie"]
        picker.delegate = self
        present(picker, animated: true)
    }

    @objc private func uploadTapped() {
        guard selectedVideoURL != nil else {
            selectVideo()
            return
        }

        // Simulate upload
        progressView.isHidden = false
        progressLabel.isHidden = false
        uploadButton.isEnabled = false

        Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] timer in
            guard let self = self else { timer.invalidate(); return }
            let p = self.progressView.progress + 0.02
            self.progressView.setProgress(p, animated: true)
            self.progressLabel.text = "Uploading... \(Int(p * 100))%"

            if p >= 1.0 {
                timer.invalidate()
                self.progressLabel.text = "Upload Complete!"
                self.uploadButton.isEnabled = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    self.navigationController?.popViewController(animated: true)
                }
            }
        }
    }

    // MARK: - UIImagePickerControllerDelegate

    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        selectedVideoURL = info[.mediaURL] as? URL
        uploadLabel.text = selectedVideoURL?.lastPathComponent ?? "Video Selected"
        uploadIcon.image = UIImage(systemName: "checkmark.circle.fill")
        picker.dismiss(animated: true)
    }
}
