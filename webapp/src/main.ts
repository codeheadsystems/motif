import { validateToken } from './auth';
import { render } from './app';

async function init(): Promise<void> {
  // Check if an existing session cookie is valid
  await validateToken();
  render();
}

init();
