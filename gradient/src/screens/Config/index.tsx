import React, { useState } from 'react';
import { useMutation, useQuery } from 'react-query';
import styled from 'styled-components';
import { SplashScreen } from '..';
import { getSignConfig } from '../../api/automation';
import { Loc } from '../../api/types';
import { LocText } from '../../common';
import { executeOperation } from '../../helpers';

export const Config = () => {
  const { isLoading, isError, data } = useQuery('sign_config', getSignConfig, {
    refetchInterval: 1000 * 5,
  });

  const { mutate, status } = useMutation('rescan', async (loc: Loc) => {
    await executeOperation(
      {
        type: 'ScanSigns',
        location: loc,
      },
      'UserInteractive',
    );
  });

  const [currentlyExpanded, setCurrentlyExpanded] = useState<string | null>(
    null,
  );

  if (isLoading || !data)
    return <SplashScreen message="Loading configuration..." />;
  if (isError) return <SplashScreen message="Failed to load configuration" />;

  return (
    <Container>
      <h1>Pathfinding Nodes</h1>
      <List>
        {Object.values(data.data.nodes)
          .sort((a, b) => {
            if (a.name < b.name) {
              return -1;
            } else if (a.name > b.name) {
              return 1;
            } else {
              return 0;
            }
          })
          .map((node) => {
            const thisNodeExpanded = currentlyExpanded === node.name;

            const toggleExpand = () => {
              thisNodeExpanded
                ? setCurrentlyExpanded(null)
                : setCurrentlyExpanded(node.name);
            };

            const rescan: React.MouseEventHandler = (ev) => {
              mutate(node.location);
              ev.stopPropagation();
            };

            return (
              <ListItem key={node.name} onClick={toggleExpand}>
                <p>{node.name}{node.home ? ' (H)' : ''}</p>
                {thisNodeExpanded && (
                  <ExpandContainer>
                    {status === 'loading' ? (
                      '(busy)'
                    ) : (
                      <button onClick={rescan}>Re-scan Location</button>
                    )}
                    <p>
                      Location: <LocText location={node.location} />
                    </p>
                    <p>
                      Drop-off location:{' '}
                      {node.dropoff ? (
                        <LocText location={node.dropoff} />
                      ) : (
                        '(none)'
                      )}
                    </p>
                    <p>
                      Pickup location:{' '}
                      {node.pickup ? (
                        <LocText location={node.pickup} />
                      ) : (
                        '(none)'
                      )}
                    </p>
                  </ExpandContainer>
                )}
              </ListItem>
            );
          })}
      </List>
      <h1>Sign Parsing and Validation Errors</h1>
      <ul>
        {data.data.validation_errors.map((err, idx) => {
          if (err.type === 'DuplicatePathfindingNode') {
            return (
              <li key={idx}>
                Attempt to create duplicate node with name {err.name}
              </li>
            );
          } else if (err.type === 'UnknownNode') {
            return (
              <li key={idx}>
                Node {err.name} was referenced, but it is unknown.
              </li>
            );
          } else {
            return <li key={idx}>Unknown error type</li>;
          }
        })}

        {data.data.sign_parse_errors.map((err, idx) => {
          if (err.type === 'NoMarker') {
            return null;
          } else if (err.type === 'NameEmpty') {
            return <li key={idx}>Expected name, but no name was found</li>;
          } else if (err.type === 'OffsetParseFailed') {
            return <li key={idx}>Failed to parse location offset</li>;
          } else if (err.type === 'UnknownSignType') {
            return <li key={idx}>Unknown sign type specified</li>;
          } else {
            return <li key={idx}>Unknown error type</li>;
          }
        })}
      </ul>
    </Container>
  );
};

const List = styled.ul`
  list-style-type: none;
  padding: 0;
`;

const ListItem = styled.li`
  padding: 10px;
  background-color: ${({ theme }) => theme.bg2};
  border-radius: 5px;
  margin-bottom: 10px;

  & > p {
    margin: 0;
  }
`;

const Container = styled.div`
  margin: 10px;
`;

const ExpandContainer = styled.div`
  & > p {
    margin: 0;
    margin-top: 10px;
  }

  & > * {
    margin-top: 10px;
  }
`;
