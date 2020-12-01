/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Component, HostListener, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import '@gravitee/ui-components/wc/gv-policy-studio';
import {OrganizationService} from "../../../services/organization.service";
import {DomainService} from "../../../services/domain.service";

@Component({
  selector: 'app-domain-flows',
  templateUrl: './flows.component.html',
  styleUrls: ['./flows.component.scss']
})
export class DomainSettingsFlowsComponent implements OnInit {
  private domainId: string;
  policies: any[];
  definition: any = {};
  flowSettingsForm: string;
  documentation: string;

  @ViewChild('studio', {static: true}) studio;

  constructor(private route: ActivatedRoute,
              private organizationService: OrganizationService,
              private domainService: DomainService,
  ) {
  }

  ngOnInit(): void {
    this.domainId = this.route.snapshot.parent.parent.params['domainId'];
    this.policies = this.route.snapshot.data['policies'] || [];
    this.flowSettingsForm = this.route.snapshot.data['flowSettingsForm'];
    this.definition.flows = this.route.snapshot.data['flows'] || [];
  }

  @HostListener(':gv-policy-studio:fetch-documentation', ['$event.detail'])
  onFetchDocumentation(detail) {
    const policy = detail.policy;
    this.studio.nativeElement.removeAttribute('documentation');
    this.organizationService.policyDocumentation(policy.id).subscribe((response) => {
      this.studio.nativeElement.setAttribute('documentation', JSON.stringify({
        content: response,
        image: policy.icon,
        id: policy.id
      }));
    }, () => {
    });
  }

  @HostListener(':gv-policy-studio:save', ['$event.detail'])
  onSave({definition}) {
    this.domainService.updateFlows(this.domainId, definition.flows).subscribe((flows) => {
      this.definition = { flows };
    });
  }

}

