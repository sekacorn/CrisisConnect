# Contributing to CrisisConnect

Thank you for your interest in contributing to CrisisConnect! This document provides guidelines for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Prioritize user safety and privacy
- Follow security best practices
- Maintain professional communication

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment
4. Create a feature branch
5. Make your changes
6. Test thoroughly
7. Submit a pull request

## Development Setup

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Maven 3.9 or higher
- PostgreSQL 15 or higher
- Docker (optional)

### Local Development

1. Clone the repository:
```bash
git clone https://github.com/sekacorn/CrisisConnect.git
cd crisisconnect
```

2. Set up environment variables:
```bash
cp .env.example .env
# Edit .env with your local configuration
```

3. Start the database:
```bash
docker-compose up db -d
```

4. Run the backend:
```bash
cd backend
mvn spring-boot:run
```

5. Run the frontend:
```bash
cd frontend
npm install
npm start
```

## Contribution Guidelines

### Code Style

**Java/Spring Boot:**
- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and small
- Use dependency injection
- Follow SOLID principles

**React/TypeScript:**
- Use functional components
- Follow React hooks best practices
- Use TypeScript types explicitly
- Keep components focused and reusable
- Use meaningful component and variable names

### Security Requirements

- **Never commit secrets** or credentials
- **Never log PII** or sensitive information
- **Validate all inputs** on both client and server
- **Sanitize outputs** to prevent XSS
- **Use parameterized queries** to prevent SQL injection
- **Follow the principle of least privilege**
- **Implement proper error handling** without exposing sensitive details

### Testing

- Write unit tests for new features
- Ensure existing tests pass
- Test edge cases and error conditions
- Test security controls
- Test privacy filtering

### Documentation

- Update README.md for new features
- Add inline comments for complex logic
- Update API documentation
- Include examples where helpful
- Document security considerations

## Pull Request Process

1. **Before Creating PR:**
   - Ensure your code compiles without errors
   - Run all tests and ensure they pass
   - Follow code style guidelines
   - Update documentation
   - Test your changes thoroughly

2. **Creating the PR:**
   - Use a clear, descriptive title
   - Describe what changes you made and why
   - Reference any related issues
   - Include screenshots for UI changes
   - List any breaking changes

3. **PR Review Process:**
   - Maintainers will review your PR
   - Address feedback and comments
   - Make requested changes
   - Keep the conversation constructive

4. **After Merge:**
   - Delete your feature branch
   - Update your local repository
   - Celebrate your contribution!

## Areas for Contribution

### High Priority

- Security enhancements
- Privacy features
- Test coverage
- Documentation improvements
- Bug fixes
- Accessibility improvements

### Feature Development

- Mobile application
- WhatsApp/SMS integration
- Multi-language support
- GIS mapping
- Advanced reporting
- SSO integration

### Infrastructure

- CI/CD pipeline
- Automated testing
- Performance optimization
- Monitoring and alerting
- Deployment automation

## Reporting Bugs

When reporting bugs, please include:

- **Description**: Clear description of the issue
- **Steps to Reproduce**: Detailed steps to reproduce
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**: OS, browser, versions
- **Screenshots**: If applicable
- **Logs**: Relevant error messages (redact sensitive info)

## Feature Requests

When requesting features:

- **Use Case**: Describe the problem you're solving
- **Proposed Solution**: How you envision it working
- **Alternatives**: Other approaches considered
- **Impact**: Who benefits from this feature
- **Priority**: How important is this feature

## Security Vulnerabilities

**DO NOT** open public issues for security vulnerabilities.

Please email sekacorn@gmail.com with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

See SECURITY.md for more details.

## License

By contributing to CrisisConnect, you agree that your contributions will be licensed under the MIT License.

## Questions?

- Open a GitHub issue for general questions
- Email the maintainers for specific concerns
- Join our community forum for discussions

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project documentation

Thank you for helping make CrisisConnect better!
