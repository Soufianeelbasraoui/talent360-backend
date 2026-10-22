CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    location VARCHAR(150),
    linkedin VARCHAR(255),
    github VARCHAR(255),
    bio TEXT,
    skills TEXT,
    experience_years INT,
    education VARCHAR(255),
    preferences_json TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_candidate_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(100),
    uploaded_at DATETIME NOT NULL,
    CONSTRAINT fk_document_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description LONGTEXT NOT NULL,
    department_id BIGINT NOT NULL,
    recruiter_id BIGINT NOT NULL,
    location VARCHAR(150) NOT NULL,
    contract_type VARCHAR(30) NOT NULL,
    work_mode VARCHAR(30) NOT NULL,
    experience_level VARCHAR(30) NOT NULL,
    required_skills TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    closed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_job_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_job_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_offer_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_offer_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    changes_summary VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_version_job FOREIGN KEY (job_offer_id) REFERENCES job_offers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    job_offer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    matching_score DOUBLE,
    applied_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_application_candidate_job UNIQUE (candidate_id, job_offer_id),
    CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE,
    CONSTRAINT fk_application_job FOREIGN KEY (job_offer_id) REFERENCES job_offers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cv_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_document_id BIGINT NOT NULL UNIQUE,
    extracted_skills_json TEXT,
    experience_years INT,
    education VARCHAR(255),
    languages_json TEXT,
    certifications_json TEXT,
    analyzed_at DATETIME NOT NULL,
    CONSTRAINT fk_analysis_document FOREIGN KEY (candidate_document_id) REFERENCES candidate_documents (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_generations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    related_entity_id BIGINT,
    related_entity_type VARCHAR(100),
    prompt_used LONGTEXT,
    generated_content LONGTEXT,
    created_by_user_id BIGINT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ai_user FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_job_status ON job_offers (status);
CREATE INDEX idx_job_department ON job_offers (department_id);
CREATE INDEX idx_job_recruiter ON job_offers (recruiter_id);
CREATE INDEX idx_app_candidate ON applications (candidate_id);
CREATE INDEX idx_app_job ON applications (job_offer_id);
CREATE INDEX idx_app_score ON applications (matching_score);
