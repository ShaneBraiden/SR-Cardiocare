// AddPatientViewController.swift
// SR-Cardiocare — Add New Patient Form (Doctor-side)
// Stitch Screen: 35d0673b (05_add_patient)
// Shows: form fields for Patient ID, Full Name, Age, Gender, Injury Type, Notes, Save button

import UIKit

final class AddPatientViewController: UIViewController {

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
        l.text = NSLocalizedString("add_patient_title", comment: "")
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

    private let formStack: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.spacing = DesignTokens.Spacing.lg
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Form fields
    private let patientIdField = FormTextField(label: NSLocalizedString("patient_id", comment: ""), placeholder: "e.g. PID-2024-001", icon: "person.badge.key")
    private let fullNameField = FormTextField(label: NSLocalizedString("full_name", comment: ""), placeholder: "e.g. John Doe", icon: "person")
    private let ageField = FormTextField(label: NSLocalizedString("age", comment: ""), placeholder: "e.g. 32", icon: "calendar")
    private let emailField = FormTextField(label: NSLocalizedString("email", comment: ""), placeholder: "e.g. john@email.com", icon: "envelope")
    private let phoneField = FormTextField(label: NSLocalizedString("phone", comment: ""), placeholder: "e.g. +1 555 123 4567", icon: "phone")

    // Gender picker
    private let genderLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("gender", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let genderSegment: UISegmentedControl = {
        let sc = UISegmentedControl(items: ["Male", "Female", "Other"])
        sc.selectedSegmentIndex = 0
        sc.selectedSegmentTintColor = DesignTokens.Colors.primary
        sc.setTitleTextAttributes([.foregroundColor: UIColor.white], for: .selected)
        sc.setTitleTextAttributes([.foregroundColor: DesignTokens.Colors.textMain], for: .normal)
        sc.translatesAutoresizingMaskIntoConstraints = false
        return sc
    }()

    // Injury type
    private let injuryTypeField = FormTextField(label: NSLocalizedString("injury_type", comment: ""), placeholder: "e.g. ACL Injury", icon: "cross.case")

    // Notes
    private let notesLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("additional_notes", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
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

    private let saveButton: UIButton = {
        let b = UIButton(type: .system)
        b.setTitle(NSLocalizedString("save_patient", comment: ""), for: .normal)
        b.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        b.setTitleColor(.white, for: .normal)
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = DesignTokens.Radius.button
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let loadingIndicator: UIActivityIndicatorView = {
        let ai = UIActivityIndicatorView(style: .medium)
        ai.color = .white
        ai.hidesWhenStopped = true
        ai.translatesAutoresizingMaskIntoConstraints = false
        return ai
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

        // Header bar
        let headerBar = UIView()
        headerBar.backgroundColor = DesignTokens.Colors.surfaceLight
        headerBar.translatesAutoresizingMaskIntoConstraints = false
        headerBar.addSubviews(backButton, headerTitle)

        // Gender row
        let genderRow = UIView()
        genderRow.translatesAutoresizingMaskIntoConstraints = false
        genderRow.addSubviews(genderLabel, genderSegment)
        NSLayoutConstraint.activate([
            genderLabel.topAnchor.constraint(equalTo: genderRow.topAnchor),
            genderLabel.leadingAnchor.constraint(equalTo: genderRow.leadingAnchor),
            genderSegment.topAnchor.constraint(equalTo: genderLabel.bottomAnchor, constant: 8),
            genderSegment.leadingAnchor.constraint(equalTo: genderRow.leadingAnchor),
            genderSegment.trailingAnchor.constraint(equalTo: genderRow.trailingAnchor),
            genderSegment.heightAnchor.constraint(equalToConstant: 44),
            genderSegment.bottomAnchor.constraint(equalTo: genderRow.bottomAnchor),
        ])

        // Notes row
        let notesRow = UIView()
        notesRow.translatesAutoresizingMaskIntoConstraints = false
        notesRow.addSubviews(notesLabel, notesTextView)
        NSLayoutConstraint.activate([
            notesLabel.topAnchor.constraint(equalTo: notesRow.topAnchor),
            notesLabel.leadingAnchor.constraint(equalTo: notesRow.leadingAnchor),
            notesTextView.topAnchor.constraint(equalTo: notesLabel.bottomAnchor, constant: 8),
            notesTextView.leadingAnchor.constraint(equalTo: notesRow.leadingAnchor),
            notesTextView.trailingAnchor.constraint(equalTo: notesRow.trailingAnchor),
            notesTextView.heightAnchor.constraint(equalToConstant: 100),
            notesTextView.bottomAnchor.constraint(equalTo: notesRow.bottomAnchor),
        ])

        // Assemble form
        formStack.addArrangedSubview(patientIdField)
        formStack.addArrangedSubview(fullNameField)
        formStack.addArrangedSubview(ageField)
        formStack.addArrangedSubview(genderRow)
        formStack.addArrangedSubview(emailField)
        formStack.addArrangedSubview(phoneField)
        formStack.addArrangedSubview(injuryTypeField)
        formStack.addArrangedSubview(notesRow)
        formStack.addArrangedSubview(saveButton)

        saveButton.heightAnchor.constraint(equalToConstant: 56).isActive = true
        saveButton.addSubview(loadingIndicator)
        loadingIndicator.centerXAnchor.constraint(equalTo: saveButton.centerXAnchor).isActive = true
        loadingIndicator.centerYAnchor.constraint(equalTo: saveButton.centerYAnchor).isActive = true

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
        saveButton.addTarget(self, action: #selector(saveTapped), for: .touchUpInside)
        ageField.textField.keyboardType = .numberPad
        emailField.textField.keyboardType = .emailAddress
        phoneField.textField.keyboardType = .phonePad
    }

    // MARK: - Actions

    @objc private func backTapped() {
        navigationController?.popViewController(animated: true)
    }

    @objc private func saveTapped() {
        guard let name = fullNameField.textField.text, !name.isEmpty else {
            showError(NSLocalizedString("error_name_required", comment: ""))
            return
        }

        saveButton.setTitle("", for: .normal)
        loadingIndicator.startAnimating()
        saveButton.isEnabled = false

        // TODO: API call to save patient
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            self?.loadingIndicator.stopAnimating()
            self?.saveButton.setTitle(NSLocalizedString("save_patient", comment: ""), for: .normal)
            self?.saveButton.isEnabled = true
            self?.navigationController?.popViewController(animated: true)
        }
    }

    private func showError(_ message: String) {
        let alert = UIAlertController(title: NSLocalizedString("error", comment: ""), message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("ok", comment: ""), style: .default))
        present(alert, animated: true)
    }
}

// MARK: - FormTextField Reusable Component

final class FormTextField: UIView {

    let textField: UITextField

    init(label: String, placeholder: String, icon: String) {
        textField = UITextField()
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false

        let lbl = UILabel()
        lbl.text = label
        lbl.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        lbl.textColor = DesignTokens.Colors.textMain
        lbl.translatesAutoresizingMaskIntoConstraints = false

        textField.placeholder = placeholder
        textField.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        textField.backgroundColor = DesignTokens.Colors.backgroundLight
        textField.layer.cornerRadius = DesignTokens.Radius.xl
        textField.translatesAutoresizingMaskIntoConstraints = false

        let iconIV = UIImageView(image: UIImage(systemName: icon))
        iconIV.tintColor = DesignTokens.Colors.textSub
        let leftContainer = UIView(frame: CGRect(x: 0, y: 0, width: 44, height: 20))
        iconIV.frame = CGRect(x: 12, y: 0, width: 20, height: 20)
        leftContainer.addSubview(iconIV)
        textField.leftView = leftContainer
        textField.leftViewMode = .always

        addSubviews(lbl, textField)

        NSLayoutConstraint.activate([
            lbl.topAnchor.constraint(equalTo: topAnchor),
            lbl.leadingAnchor.constraint(equalTo: leadingAnchor),

            textField.topAnchor.constraint(equalTo: lbl.bottomAnchor, constant: 8),
            textField.leadingAnchor.constraint(equalTo: leadingAnchor),
            textField.trailingAnchor.constraint(equalTo: trailingAnchor),
            textField.heightAnchor.constraint(equalToConstant: 48),
            textField.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }

    required init?(coder: NSCoder) { fatalError() }
}
