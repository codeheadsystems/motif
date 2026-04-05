import { isLoggedIn, validateToken } from './auth';
import { render } from './app';

async function init(): Promise<void> {
  if (isLoggedIn()) {
    await validateToken();
  }
  render();
}

init();
