// ExerciseLibraryViewController.swift
// SR-Cardiocare — Exercise Library (Doctor-side)
// Stitch Screen: 444b4e64 (07_exercise_library)
// Shows: search bar, filter chips, grid/list of exercises with thumbnails

import UIKit

final class ExerciseLibraryViewController: UIViewController {

    // MARK: - Data

    private let categories = ["All", "Knee", "Shoulder", "Post-Op", "Back", "Ankle"]
    private var selectedCategory = 0

    private let exercises: [Exercise] = [
        Exercise(id: "1", name: "Quad Strengthening", description: "Build quadriceps strength", category: "Knee", difficultyLevel: "Intermediate", durationSeconds: 120, videoURL: nil, thumbnailURL: nil, sets: 3, reps: 12, instructions: []),
        Exercise(id: "2", name: "Shoulder Press", description: "Overhead shoulder mobility", category: "Shoulder", difficultyLevel: "Advanced", durationSeconds: 90, videoURL: nil, thumbnailURL: nil, sets: 3, reps: 10, instructions: []),
        Exercise(id: "3", name: "Wall Slides", description: "Post-op recovery motion", category: "Post-Op", difficultyLevel: "Beginner", durationSeconds: 60, videoURL: nil, thumbnailURL: nil, sets: 2, reps: 15, instructions: []),
        Exercise(id: "4", name: "Cat-Cow Stretch", description: "Spinal flexibility", category: "Back", difficultyLevel: "Beginner", durationSeconds: 120, videoURL: nil, thumbnailURL: nil, sets: 2, reps: 10, instructions: []),
        Exercise(id: "5", name: "Ankle Circles", description: "Range of motion exercise", category: "Ankle", difficultyLevel: "Beginner", durationSeconds: 60, videoURL: nil, thumbnailURL: nil, sets: 3, reps: 20, instructions: []),
        Exercise(id: "6", name: "Hamstring Curl", description: "Posterior chain strength", category: "Knee", difficultyLevel: "Intermediate", durationSeconds: 90, videoURL: nil, thumbnailURL: nil, sets: 3, reps: 12, instructions: []),
    ]

    private var filteredExercises: [Exercise] = []

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
        l.text = NSLocalizedString("exercise_library_title", comment: "")
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: .bold)
        l.textColor = DesignTokens.Colors.textMain
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let searchField: UITextField = {
        let tf = UITextField()
        tf.placeholder = NSLocalizedString("search_exercises", comment: "")
        tf.font = DesignTokens.Typography.inter(DesignTokens.Typography.body)
        tf.backgroundColor = DesignTokens.Colors.backgroundLight
        tf.layer.cornerRadius = DesignTokens.Radius.xl
        let iv = UIImageView(image: UIImage(systemName: "magnifyingglass"))
        iv.tintColor = DesignTokens.Colors.textSub
        let container = UIView(frame: CGRect(x: 0, y: 0, width: 44, height: 20))
        iv.frame = CGRect(x: 16, y: 0, width: 20, height: 20)
        container.addSubview(iv)
        tf.leftView = container
        tf.leftViewMode = .always
        tf.translatesAutoresizingMaskIntoConstraints = false
        return tf
    }()

    private lazy var chipCollectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.scrollDirection = .horizontal
        layout.estimatedItemSize = UICollectionViewFlowLayout.automaticSize
        layout.minimumInteritemSpacing = 8
        layout.sectionInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 16)
        let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
        cv.backgroundColor = DesignTokens.Colors.surfaceLight
        cv.showsHorizontalScrollIndicator = false
        cv.dataSource = self
        cv.delegate = self
        cv.register(ChipCell.self, forCellWithReuseIdentifier: ChipCell.reuseID)
        cv.translatesAutoresizingMaskIntoConstraints = false
        return cv
    }()

    private lazy var exerciseCollectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.minimumInteritemSpacing = DesignTokens.Spacing.base
        layout.minimumLineSpacing = DesignTokens.Spacing.base
        layout.sectionInset = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
        cv.backgroundColor = .clear
        cv.dataSource = self
        cv.delegate = self
        cv.register(ExerciseGridCell.self, forCellWithReuseIdentifier: ExerciseGridCell.reuseID)
        cv.translatesAutoresizingMaskIntoConstraints = false
        return cv
    }()

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        filteredExercises = exercises
        setupUI()
        setupActions()
    }

    private func setupUI() {
        view.backgroundColor = DesignTokens.Colors.backgroundLight
        navigationController?.setNavigationBarHidden(true, animated: false)

        let headerBar = UIView()
        headerBar.backgroundColor = DesignTokens.Colors.surfaceLight
        headerBar.translatesAutoresizingMaskIntoConstraints = false
        headerBar.addSubviews(backButton, headerTitle, searchField)

        view.addSubviews(headerBar, chipCollectionView, exerciseCollectionView)

        NSLayoutConstraint.activate([
            headerBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            headerBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            backButton.topAnchor.constraint(equalTo: headerBar.topAnchor, constant: 8),
            backButton.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor, constant: 8),
            backButton.widthAnchor.constraint(equalToConstant: 44),
            backButton.heightAnchor.constraint(equalToConstant: 44),

            headerTitle.centerYAnchor.constraint(equalTo: backButton.centerYAnchor),
            headerTitle.centerXAnchor.constraint(equalTo: headerBar.centerXAnchor),

            searchField.topAnchor.constraint(equalTo: backButton.bottomAnchor, constant: 8),
            searchField.leadingAnchor.constraint(equalTo: headerBar.leadingAnchor, constant: 16),
            searchField.trailingAnchor.constraint(equalTo: headerBar.trailingAnchor, constant: -16),
            searchField.heightAnchor.constraint(equalToConstant: 48),
            searchField.bottomAnchor.constraint(equalTo: headerBar.bottomAnchor, constant: -8),

            chipCollectionView.topAnchor.constraint(equalTo: headerBar.bottomAnchor),
            chipCollectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            chipCollectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            chipCollectionView.heightAnchor.constraint(equalToConstant: 52),

            exerciseCollectionView.topAnchor.constraint(equalTo: chipCollectionView.bottomAnchor),
            exerciseCollectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            exerciseCollectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            exerciseCollectionView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    private func setupActions() {
        backButton.addTarget(self, action: #selector(backTapped), for: .touchUpInside)
        searchField.addTarget(self, action: #selector(searchChanged), for: .editingChanged)
    }

    private func filterExercises() {
        let query = searchField.text?.lowercased() ?? ""
        let cat = categories[selectedCategory]
        filteredExercises = exercises.filter {
            let matchesCat = cat == "All" || $0.category == cat
            let matchesSearch = query.isEmpty || $0.name.lowercased().contains(query)
            return matchesCat && matchesSearch
        }
        exerciseCollectionView.reloadData()
    }

    @objc private func backTapped() { navigationController?.popViewController(animated: true) }
    @objc private func searchChanged() { filterExercises() }
}

// MARK: - UICollectionView DataSource + Delegate

extension ExerciseLibraryViewController: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        collectionView == chipCollectionView ? categories.count : filteredExercises.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if collectionView == chipCollectionView {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: ChipCell.reuseID, for: indexPath) as! ChipCell
            cell.configure(title: categories[indexPath.item], isActive: indexPath.item == selectedCategory)
            return cell
        } else {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: ExerciseGridCell.reuseID, for: indexPath) as! ExerciseGridCell
            cell.configure(with: filteredExercises[indexPath.item])
            return cell
        }
    }

    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        if collectionView == chipCollectionView {
            let text = categories[indexPath.item]
            let width = text.size(withAttributes: [.font: DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)]).width + 32
            return CGSize(width: width, height: 36)
        } else {
            let totalWidth = collectionView.bounds.width - 48
            let width = totalWidth / 2
            return CGSize(width: width, height: width * 1.2)
        }
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        if collectionView == chipCollectionView {
            selectedCategory = indexPath.item
            chipCollectionView.reloadData()
            filterExercises()
        }
    }
}

// MARK: - ChipCell

final class ChipCell: UICollectionViewCell {
    static let reuseID = "ChipCell"

    private let label: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .medium)
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.layer.cornerRadius = 18
        contentView.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            label.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            label.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
        ])
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(title: String, isActive: Bool) {
        label.text = title
        if isActive {
            contentView.backgroundColor = DesignTokens.Colors.primary
            label.textColor = .white
        } else {
            contentView.backgroundColor = DesignTokens.Colors.neutralLight.withAlphaComponent(0.4)
            label.textColor = DesignTokens.Colors.textSub
        }
    }
}

// MARK: - ExerciseGridCell

final class ExerciseGridCell: UICollectionViewCell {
    static let reuseID = "ExerciseGridCell"

    private let thumbnailView: UIView = {
        let v = UIView()
        v.backgroundColor = DesignTokens.Colors.neutralLight
        v.layer.cornerRadius = DesignTokens.Radius.lg
        v.clipsToBounds = true
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let durationLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.caption, weight: .medium)
        l.textColor = .white
        l.backgroundColor = DesignTokens.Colors.slate800.withAlphaComponent(0.75)
        l.layer.cornerRadius = 4
        l.clipsToBounds = true
        l.textAlignment = .center
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    private let nameLabel: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(DesignTokens.Typography.subheadline, weight: .semibold)
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

    private let difficultyBadge: UILabel = {
        let l = UILabel()
        l.font = DesignTokens.Typography.inter(10, weight: .semibold)
        l.textAlignment = .center
        l.layer.cornerRadius = 8
        l.clipsToBounds = true
        l.translatesAutoresizingMaskIntoConstraints = false
        return l
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.backgroundColor = DesignTokens.Colors.surfaceLight
        contentView.layer.cornerRadius = DesignTokens.Radius.xxl
        contentView.layer.borderWidth = 1
        contentView.layer.borderColor = DesignTokens.Colors.neutralLight.cgColor
        contentView.clipsToBounds = true

        contentView.addSubviews(thumbnailView, durationLabel, nameLabel, detailLabel, difficultyBadge)

        NSLayoutConstraint.activate([
            thumbnailView.topAnchor.constraint(equalTo: contentView.topAnchor),
            thumbnailView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            thumbnailView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            thumbnailView.heightAnchor.constraint(equalTo: contentView.widthAnchor, multiplier: 0.5625),

            durationLabel.bottomAnchor.constraint(equalTo: thumbnailView.bottomAnchor, constant: -8),
            durationLabel.trailingAnchor.constraint(equalTo: thumbnailView.trailingAnchor, constant: -8),
            durationLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 36),
            durationLabel.heightAnchor.constraint(equalToConstant: 20),

            nameLabel.topAnchor.constraint(equalTo: thumbnailView.bottomAnchor, constant: 10),
            nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 12),
            nameLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -12),

            detailLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 4),
            detailLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),

            difficultyBadge.topAnchor.constraint(equalTo: detailLabel.bottomAnchor, constant: 8),
            difficultyBadge.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            difficultyBadge.widthAnchor.constraint(greaterThanOrEqualToConstant: 56),
            difficultyBadge.heightAnchor.constraint(equalToConstant: 20),
        ])
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with exercise: Exercise) {
        nameLabel.text = exercise.name
        detailLabel.text = "\(exercise.sets ?? 0) Sets • \(exercise.reps ?? 0) Reps"

        let mins = exercise.durationSeconds / 60
        let secs = exercise.durationSeconds % 60
        durationLabel.text = " \(mins):\(String(format: "%02d", secs)) "

        difficultyBadge.text = " \(exercise.difficultyLevel) "
        switch exercise.difficultyLevel.lowercased() {
        case "beginner":
            difficultyBadge.textColor = DesignTokens.Colors.success
            difficultyBadge.backgroundColor = DesignTokens.Colors.success.withAlphaComponent(0.1)
        case "intermediate":
            difficultyBadge.textColor = DesignTokens.Colors.warning
            difficultyBadge.backgroundColor = DesignTokens.Colors.warning.withAlphaComponent(0.1)
        case "advanced":
            difficultyBadge.textColor = DesignTokens.Colors.error
            difficultyBadge.backgroundColor = DesignTokens.Colors.error.withAlphaComponent(0.1)
        default:
            difficultyBadge.textColor = DesignTokens.Colors.textSub
            difficultyBadge.backgroundColor = DesignTokens.Colors.neutralLight.withAlphaComponent(0.3)
        }
    }
}
