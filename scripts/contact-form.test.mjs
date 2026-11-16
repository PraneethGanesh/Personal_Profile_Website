import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import ts from 'typescript';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { test } from 'node:test';
import assert from 'node:assert/strict';

const source = await readFile(new URL('../src/app/components/contact/contact.component.ts', import.meta.url), 'utf8');
const environment = await readFile(new URL('../src/environments/environment.ts', import.meta.url), 'utf8');
const configUrl = 'data:text/javascript;base64,' + Buffer.from(ts.transpile(environment, { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 })).toString('base64');
const code = ts.transpile(source, { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022, experimentalDecorators: true })
  .replace('../../../environments/environment', configUrl);
const output = new URL('../verification-output/contact.component.mjs', import.meta.url);
await mkdir(new URL('../verification-output/', import.meta.url), { recursive: true });
await writeFile(output, code);
const { ContactComponent } = await import(output.href);
function setup() {
  const response = new Subject();
  const calls = [];
  const injector = createEnvironmentInjector([{ provide: HttpClient, useValue: { post: (...args) => { calls.push(args); return response; } } }]);
  const component = runInInjectionContext(injector, () => new ContactComponent());
  const form = { invalid: false, value: { fullName: ' Visitor ', email: 'visitor@example.test', message: ' Hello ' }, resetForm() { this.value = {}; this.resets++; }, resets: 0 };
  return { component, form, calls, response, injector };
}
test('submits existing fields, blocks pending resubmission, accepts only 202 and clears on success', () => {
  const { component, form, calls, response, injector } = setup();
  component.submitMessage(form);
  assert.equal(component.pending(), true);
  component.submitMessage(form);
  component.clearMessage(form);
  assert.equal(calls.length, 1);
  assert.match(calls[0][0], /\/api\/contact$/);
  assert.deepEqual(calls[0][1], { name: 'Visitor', email: 'visitor@example.test', message: 'Hello' });
  assert.equal(form.resets, 0);
  response.next({ status: 202 }); response.complete();
  assert.equal(component.status(), 'Thanks. Your message was accepted');
  assert.equal(component.pending(), false);
  assert.equal(form.resets, 1);
  injector.destroy();
});
test('retains input and allows retry after network and rate-limit errors', () => {
  for (const status of [0, 429, 503]) {
    const { component, form, response, injector } = setup();
    const values = { ...form.value };
    component.submitMessage(form);
    response.error(new HttpErrorResponse({ status }));
    assert.equal(component.pending(), false);
    assert.equal(component.failed(), true);
    assert.deepEqual(form.value, values);
    assert.equal(form.resets, 0);
    injector.destroy();
  }
});
test('retains input for unexpected success status and rejects invalid forms', () => {
  const { component, form, calls, response, injector } = setup();
  form.invalid = true; component.submitMessage(form);
  assert.equal(calls.length, 0);
  form.invalid = false; component.submitMessage(form);
  response.next({ status: 200 }); response.complete();
  assert.equal(component.failed(), true);
  assert.equal(form.resets, 0);
  injector.destroy();
});
