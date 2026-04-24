.DEFAULT_GOAL := help
.PHONY: help install build build-backend build-infra test test-backend test-infra synth clean

help:
	@echo "Motif — common tasks"
	@echo ""
	@echo "  make install         Install npm deps for webapp and infra"
	@echo "  make build           Build everything: backend (+ Android later), webapp, CDK"
	@echo "  make build-backend   Gradle assemble (Java + webapp, no tests)"
	@echo "  make build-infra     Type-check TypeScript and synth all CDK stacks"
	@echo "  make test            Run all tests: backend, webapp, CDK"
	@echo "  make test-backend    Gradle test (covers server, server-db, common, webapp unit)"
	@echo "  make test-infra      Jest tests for CDK stacks"
	@echo "  make synth           cdk synth --all (writes infra/cdk.out/)"
	@echo "  make clean           Remove build artifacts and node_modules"

install:
	cd webapp && npm ci
	cd infra && npm ci

# Order: install deps first so gradle's buildWebapp (npm run build) finds node_modules
# and the infra TypeScript compile has @types/node available.
build: install build-backend build-infra

build-backend:
	./gradlew assemble

build-infra:
	cd infra && npx tsc --noEmit
	cd infra && npx cdk synth --all --quiet

test: install test-backend test-infra

test-backend:
	./gradlew test

test-infra:
	cd infra && npm test

synth: install
	cd infra && npx cdk synth --all

clean:
	./gradlew clean
	rm -rf infra/cdk.out infra/node_modules webapp/node_modules
