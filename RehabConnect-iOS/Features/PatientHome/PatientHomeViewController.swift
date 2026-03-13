// PatientHomeViewController.swift
// SR-Cardiocare — Patient Home/Dashboard
// Stitch Screen: e34f1d0a (28_patient_home)
// Shows: greeting header, progress ring (60%), daily exercises list, bottom tab bar

import UIKit

final class PatientHomeViewController: UIViewController {

    // MARK: - Models

    struct ExerciseItem {
        let name: String
        let detail: String
        let status: ExerciseStatus
    }

    enum ExerciseStatus {
        case completed, active, pending
    }

    // MARK: - Data

    private let exercises: [ExerciseItem] = [
        ExerciseItem(name: "Hamstring Stretch", detail: "2 Sets • 30 Seconds", status: .completed),
        ExerciseItem(name: "Knee Flexion", detail: "3 Sets • 10 Reps", status: .active),
        ExerciseItem(name: "Wall Squats", detail: "2 Sets • 15 Reps", status: .pending),
        ExerciseItem(name: "Band Pull Aparts", detail: "3 Sets • 12 Reps", status: .pending),
        ExerciseItem(name: "Calf Raises", detail: "3 Sets • 15 Reps", status: .pending),
    ]

    // MARK: - UI

    private let scrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.showsVerticalScrollIndicator = false
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private let contentStack: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.spacing = 0
        s.translatesAutoresizingMaskIntoConstraints = false
        return s
    }()

    // Header
    private let headerView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let dateLabel: UILabel = {
        let l = UILabel()
        let df = DateFormatter()
        df.dateFormat = "EEEE, MMM d"
        l.text = df.string(from: Date())
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let greetingLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.title, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let notificationButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "bell.fill"), for: .normal)
        b.tintColor = DesignTokens.Colors.textMain
        b.backgroundColor = DesignTokens.Colors.backgroundLight
        b.layer.cornerRadius = 20
        b.translatesAutoresizingMaskIntoConstraints = false
        return b
    }()

    private let notificationBadge: UIView = {
        let v = UIView()
        v.backgroundColor = .systemRed
        v.layer.cornerRadius = 5
        v.layer.borderWidth = 2
        v.layer.borderColor = UIColor.white.cgColor
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    // Progress Ring Section
    private let progressContainer: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xxl
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let progressPercentLabel: UILabel = {
        let l = UILabel()
        l.text = "60%"
        l.font = DesignTokens.Typography.inter(36, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let progressSubLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("completed", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let progressDetailLabel: UILabel = {
        let l = UILabel()
        l.text = NSLocalizedString("patient_home_progress_detail", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline)
        l.textColor = DesignTokens.Colors.textSub
        l.textAlignment = .center
        l.numberOfLines = 0
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let progressRingLayer = CAShapeLayer()
    private let progressTrackLayer = CAShapeLayer()

    // Exercises Section
    private let exerciseTableView: UITableView = {
        let tv = UITableView(frame: .zero, style: .plain)
        tv.separatorStyle = .none
        tv.backgroundColor = .clear
        tv.isScrollEnabled = false
        tv.translatesAutoresizingMaskIntoConstraints = false
        return tv
    }()

    private var exerciseTableHeight: NSLayoutConstraint!

    // Tab Bar
    private let tabBarView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupTableView()
        configureGreeting()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        drawProgressRing()
        exerciseTableHeight.constant = CGFloat(exercises.count * 100)
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        // Header
        headerView.addSubviews(dateLabel, greetingLabel, notificationButton, notificationBadge)
        NSLayoutConstraint.activate([
            dateLabel.topAnchor.constraint(equalTo: headerView.topAnchor, constant: DesignTokens.Spacing.xl),
            dateLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: DesignTokens.Spacing.xl),

            greetingLabel.topAnchor.constraint(equalTo: dateLabel.bottomAnchor, constant: 4),
            greetingLabel.leadingAnchor.constraint(equalTo: dateLabel.leadingAnchor),
            greetingLabel.bottomAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -DesignTokens.Spacing.xl),

            notificationButton.centerYAnchor.constraint(equalTo: headerView.centerYAnchor),
            notificationButton.trailingAnchor.constraint(equalTo: headerView.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            notificationButton.widthAnchor.constraint(equalToConstant: 40),
            notificationButton.heightAnchor.constraint(equalToConstant: 40),

            notificationBadge.topAnchor.constraint(equalTo: notificationButton.topAnchor, constant: 6),
            notificationBadge.trailingAnchor.constraint(equalTo: notificationButton.trailingAnchor, constant: -6),
            notificationBadge.widthAnchor.constraint(equalToConstant: 10),
            notificationBadge.heightAnchor.constraint(equalToConstant: 10),
        ])

        // Scroll setup
        view.addSubviews(scrollView, tabBarView)
        scrollView.addSubview(contentStack)

        // "Today's Plan" label
        let planLabel = sectionHeader(NSLocalizedString("patient_home_todays_plan", comment: ""))

        // Progress card
        let progressCard = UIView()
        progressCard.translatesAutoresizingMaskIntoConstraints = false
        progressCard.addSubviews(progressContainer)
        progressContainer.addSubviews(progressPercentLabel, progressSubLabel, progressDetailLabel)

        NSLayoutConstraint.activate([
            progressContainer.topAnchor.constraint(equalTo: progressCard.topAnchor),
            progressContainer.leadingAnchor.constraint(equalTo: progressCard.leadingAnchor, constant: DesignTokens.Spacing.xl),
            progressContainer.trailingAnchor.constraint(equalTo: progressCard.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            progressContainer.bottomAnchor.constraint(equalTo: progressCard.bottomAnchor),
            progressContainer.heightAnchor.constraint(equalToConstant: 280),

            progressPercentLabel.centerXAnchor.constraint(equalTo: progressContainer.centerXAnchor),
            progressPercentLabel.centerYAnchor.constraint(equalTo: progressContainer.centerYAnchor, constant: -30),

            progressSubLabel.topAnchor.constraint(equalTo: progressPercentLabel.bottomAnchor, constant: 4),
            progressSubLabel.centerXAnchor.constraint(equalTo: progressContainer.centerXAnchor),

            progressDetailLabel.topAnchor.constraint(equalTo: progressSubLabel.bottomAnchor, constant: DesignTokens.Spacing.xl),
            progressDetailLabel.leadingAnchor.constraint(equalTo: progressContainer.leadingAnchor, constant: DesignTokens.Spacing.xl),
            progressDetailLabel.trailingAnchor.constraint(equalTo: progressContainer.trailingAnchor, constant: -DesignTokens.Spacing.xl),
        ])

        // Exercises header
        let exerciseHeaderView = UIView()
        exerciseHeaderView.translatesAutoresizingMaskIntoConstraints = false
        let exLabel = UILabel()
        exLabel.text = NSLocalizedString("patient_home_daily_exercises", comment: "")
        exLabel.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        exLabel.textColor = DesignTokens.Colors.textMain
        exLabel.translatesAutoresizingMaskIntoConstraints = false
        let viewAllBtn = UIButton(type: .system)
        viewAllBtn.setTitle(NSLocalizedString("view_all", comment: ""), for: .normal)
        viewAllBtn.titleLabel?.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
        viewAllBtn.setTitleColor(DesignTokens.Colors.primary, for: .normal)
        viewAllBtn.translatesAutoresizingMaskIntoConstraints = false
        exerciseHeaderView.addSubviews(exLabel, viewAllBtn)

        NSLayoutConstraint.activate([
            exLabel.leadingAnchor.constraint(equalTo: exerciseHeaderView.leadingAnchor, constant: DesignTokens.Spacing.xl),
            exLabel.centerYAnchor.constraint(equalTo: exerciseHeaderView.centerYAnchor),
            viewAllBtn.trailingAnchor.constraint(equalTo: exerciseHeaderView.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            viewAllBtn.centerYAnchor.constraint(equalTo: exerciseHeaderView.centerYAnchor),
            exerciseHeaderView.heightAnchor.constraint(equalToConstant: 44),
        ])

        // Exercise list container
        let exerciseContainer = UIView()
        exerciseContainer.translatesAutoresizingMaskIntoConstraints = false
        exerciseContainer.addSubview(exerciseTableView)
        exerciseTableHeight = exerciseTableView.heightAnchor.constraint(equalToConstant: CGFloat(exercises.count * 100))
        NSLayoutConstraint.activate([
            exerciseTableView.topAnchor.constraint(equalTo: exerciseContainer.topAnchor),
            exerciseTableView.leadingAnchor.constraint(equalTo: exerciseContainer.leadingAnchor, constant: DesignTokens.Spacing.xl),
            exerciseTableView.trailingAnchor.constraint(equalTo: exerciseContainer.trailingAnchor, constant: -DesignTokens.Spacing.xl),
            exerciseTableView.bottomAnchor.constraint(equalTo: exerciseContainer.bottomAnchor),
            exerciseTableHeight,
        ])

        // Assemble content stack
        contentStack.addArrangedSubview(headerView)
        contentStack.addArrangedSubview(planLabel)
        contentStack.addArrangedSubview(progressCard)
        contentStack.addArrangedSubview(exerciseHeaderView)
        contentStack.addArrangedSubview(exerciseContainer)

        contentStack.setCustomSpacing(DesignTokens.Spacing.xl, after: headerView)
        contentStack.setCustomSpacing(DesignTokens.Spacing.base, after: planLabel)
        contentStack.setCustomSpacing(DesignTokens.Spacing.xl, after: progressCard)

        // Layout
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: tabBarView.topAnchor),

            contentStack.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentStack.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentStack.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor, constant: -DesignTokens.Spacing.xl),
            contentStack.widthAnchor.constraint(equalTo: scrollView.widthAnchor),

            tabBarView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tabBarView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tabBarView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tabBarView.heightAnchor.constraint(equalToConstant: 83),
        ])

        setupTabBar()
    }

    private func setupTableView() {
        exerciseTableView.dataSource = self
        exerciseTableView.delegate = self
        exerciseTableView.register(ExerciseCell.self, forCellReuseIdentifier: ExerciseCell.reuseID)
    }

    private func configureGreeting() {
        let hour = Calendar.current.component(.hour, from: Date())
        let greeting: String
        switch hour {
        case 5..<12: greeting = NSLocalizedString("good_morning", comment: "")
        case 12..<17: greeting = NSLocalizedString("good_afternoon", comment: "")
        default: greeting = NSLocalizedString("good_evening", comment: "")
        }
        let name = UserDefaults.standard.string(forKey: "user_first_name") ?? "Sarah"
        greetingLabel.text = "\(greeting), \(name)"
    }

    private func sectionHeader(_ title: String) -> UIView {
        let wrapper = UIView()
        wrapper.translatesAutoresizingMaskIntoConstraints = false
        let l = UILabel()
        l.text = title
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        wrapper.addSubview(l)
        NSLayoutConstraint.activate([
            l.leadingAnchor.constraint(equalTo: wrapper.leadingAnchor, constant: DesignTokens.Spacing.xl),
            l.centerYAnchor.constraint(equalTo: wrapper.centerYAnchor),
            wrapper.heightAnchor.constraint(equalToConstant: 44),
        ])
        return wrapper
    }

    // MARK: - Progress Ring

    private func drawProgressRing() {
        progressTrackLayer.removeFromSuperlayer()
        progressRingLayer.removeFromSuperlayer()

        let center = CGPoint(x: progressContainer.bounds.width / 2, y: 110)
        let radius: CGFloat = 80
        let startAngle: CGFloat = -.pi / 2
        let endAngle: CGFloat = startAngle + 2 * .pi

        let trackPath = UIBezierPath(arcCenter: center, radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: true)
        progressTrackLayer.path = trackPath.cgPath
        progressTrackLayer.strokeColor = DesignTokens.Colors.neutralLight.cgColor
        progressTrackLayer.fillColor = UIColor.clear.cgColor
        progressTrackLayer.lineWidth = 10
        progressTrackLayer.lineCap = .round
        progressContainer.layer.addSublayer(progressTrackLayer)

        let progressAngle = startAngle + 2 * .pi * 0.6
        let progressPath = UIBezierPath(arcCenter: center, radius: radius, startAngle: startAngle, endAngle: progressAngle, clockwise: true)
        progressRingLayer.path = progressPath.cgPath
        progressRingLayer.strokeColor = DesignTokens.Colors.primary.cgColor
        progressRingLayer.fillColor = UIColor.clear.cgColor
        progressRingLayer.lineWidth = 10
        progressRingLayer.lineCap = .round
        progressContainer.layer.addSublayer(progressRingLayer)
    }

    // MARK: - Tab Bar

    private func setupTabBar() {
        let tabs: [(icon: String, label: String, active: Bool)] = [
            ("house.fill", "Home", true),
            ("calendar", "Schedule", false),
            ("chart.bar.fill", "Progress", false),
            ("person.fill", "Profile", false),
        ]

        let stack = UIStackView()
        stack.axis = .horizontal
        stack.distribution = .fillEqually
        stack.translatesAutoresizingMaskIntoConstraints = false

        for tab in tabs {
            let btn = UIButton(type: .system)
            let config = UIImage.SymbolConfiguration(pointSize: 22, weight: .medium)
            btn.setImage(UIImage(systemName: tab.icon, withConfiguration: config), for: .normal)
            btn.tintColor = tab.active ? DesignTokens.Colors.primary : DesignTokens.Colors.textSub
            stack.addArrangedSubview(btn)
        }

        tabBarView.addSubview(stack)
        let divider = UIView()
        divider.backgroundColor = UIColor.separator
        divider.translatesAutoresizingMaskIntoConstraints = false
        tabBarView.addSubview(divider)

        NSLayoutConstraint.activate([
            divider.topAnchor.constraint(equalTo: tabBarView.topAnchor),
            divider.leadingAnchor.constraint(equalTo: tabBarView.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: tabBarView.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 0.5),

            stack.topAnchor.constraint(equalTo: tabBarView.topAnchor, constant: 8),
            stack.leadingAnchor.constraint(equalTo: tabBarView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: tabBarView.trailingAnchor),
            stack.heightAnchor.constraint(equalToConstant: 50),
        ])
    }
}

// MARK: - UITableView

extension PatientHomeViewController: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { exercises.count }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat { 96 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: ExerciseCell.reuseID, for: indexPath) as! ExerciseCell
        cell.configure(with: exercises[indexPath.row])
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let exercise = exercises[indexPath.row]
        guard exercise.status != .completed else { return }
        let playerVC = WorkoutPlayerViewController()
        navigationController?.pushViewController(playerVC, animated: true)
    }
}

// MARK: - ExerciseCell

final class ExerciseCell: UITableViewCell {

    static let reuseID = "ExerciseCell"

    private let cardView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.surfaceLight
        v.layer.cornerRadius = DesignTokens.Radius.xl
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let thumbView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.neutralLight
        v.layer.cornerRadius = DesignTokens.Radius.lg
        v.clipsToBounds = true
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.body, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let detailLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption)
        l.textColor = DesignTokens.Colors.textSub
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let statusLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .semibold)
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let playButton: UIButton = {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: "play.fill"), for: .normal)
        b.tintColor = .white
        b.backgroundColor = DesignTokens.Colors.primary
        b.layer.cornerRadius = 20
        b.translatesAutoresizingMaskIntoConstraints = false
        b.isHidden = true
        return b
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setup()
    }

    required init?(coder: NSCoder) { fatalError() }

    private func setup() {
        backgroundColor = .clear
        selectionStyle = .none
        contentView.addSubview(cardView)
        cardView.addSubviews(thumbView, nameLabel, detailLabel, statusLabel, playButton)

        NSLayoutConstraint.activate([
            cardView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            cardView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            cardView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            cardView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -4),

            thumbView.leadingAnchor.constraint(equalTo: cardView.leadingAnchor, constant: 12),
            thumbView.centerYAnchor.constraint(equalTo: cardView.centerYAnchor),
            thumbView.widthAnchor.constraint(equalToConstant: 72),
            thumbView.heightAnchor.constraint(equalToConstant: 72),

            nameLabel.topAnchor.constraint(equalTo: thumbView.topAnchor, constant: 4),
            nameLabel.leadingAnchor.constraint(equalTo: thumbView.trailingAnchor, constant: DesignTokens.Spacing.base),
            nameLabel.trailingAnchor.constraint(lessThanOrEqualTo: playButton.leadingAnchor, constant: -8),

            detailLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            detailLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            statusLabel.topAnchor.constraint(equalTo: detailLabel.bottomAnchor, constant: 8),
            statusLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            playButton.trailingAnchor.constraint(equalTo: cardView.trailingAnchor, constant: -12),
            playButton.centerYAnchor.constraint(equalTo: cardView.centerYAnchor),
            playButton.widthAnchor.constraint(equalToConstant: 40),
            playButton.heightAnchor.constraint(equalToConstant: 40),
        ])
    }

    func configure(with item: PatientHomeViewController.ExerciseItem) {
        detailLabel.text = item.detail

        switch item.status {
        case .completed:
            nameLabel.attributedText = NSAttributedString(
                string: item.name,
                attributes: [.strikethroughStyle: NSUnderlineStyle.single.rawValue,
                             .strikethroughColor: DesignTokens.Colors.textSub]
            )
            statusLabel.text = "✓ Completed"
            statusLabel.textColor = DesignTokens.Colors.primary
            playButton.isHidden = true
            cardView.layer.borderWidth = 0

        case .active:
            nameLabel.attributedText = nil
            nameLabel.text = item.name
            statusLabel.text = "● Up Next"
            statusLabel.textColor = DesignTokens.Colors.primary
            playButton.isHidden = false
            cardView.layer.borderWidth = 0
            cardView.layer.shadowColor = UIColor.black.cgColor
            cardView.layer.shadowOpacity = 0.06
            cardView.layer.shadowRadius = 8
            cardView.layer.shadowOffset = CGSize(width: 0, height: 2)

        case .pending:
            nameLabel.attributedText = nil
            nameLabel.text = item.name
            nameLabel.alpha = 0.9
            statusLabel.text = "Pending"
            statusLabel.textColor = DesignTokens.Colors.textSub
            playButton.isHidden = true
            cardView.layer.borderWidth = 0
        }
    }
}
